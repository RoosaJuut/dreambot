package roosajuut.dreambot.scriptmain.powerminer;

import org.dreambot.api.input.Keyboard;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.listener.ChatListener;
import org.dreambot.api.wrappers.interactive.Character;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.widgets.message.Message;
import roosajuut.dreambot.powerminer.gui.ScriptVars;
import roosajuut.dreambot.powerminer.gui.minerGui;
import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.event.impl.mouse.impl.click.ClickMode;
import org.dreambot.api.input.mouse.destination.impl.EntityDestination;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.MethodProvider;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Map;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.utilities.impl.Condition;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.Menu;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static org.dreambot.api.methods.interactive.Players.*;

@ScriptManifest(author = "Hents", description = "Power Miner/Smelter", name = "DreamBot Power Miner", version = 1.86, category = Category.MINING)
public class Miner extends AbstractScript {
    Area EDGE_BANK = new Area(3095, 3496, 3095, 3494, 0);
    Area FURNACE = new Area(3109, 3499, 3108, 3498, 0);
    Area AL_KHARID_FURNACE = new Area(3275, 3186, 3276,3187, 0);
    Tile TIN_ORE_1 = new Tile(3282, 3363, 0);
    Tile TIN_ORE_2 = new Tile(3288,3367,0);
    Tile TIN_ORE_3 = new Tile(3282, 3363, 0);
    //Tile TIN_ORE_2 = new Tile(3288, 3367, 0);
    //Tile TIN_ORE_3 = new Tile(3282, 3366, 0);
    Tile COPPER_ORE_1 = new Tile(3286, 3365, 0);
    Tile COPPER_ORE_2 = new Tile(3286, 3361, 0);
    Tile COPPER_ORE_3 = new Tile(3289, 3362, 0);
    Tile DUCK_HUNTER_1 = new Tile(3151, 3382, 0);
    Tile GUARD_KILLER_1 = new Tile(3167, 3426, 0);
    Tile GUARD_KILLER_2 = new Tile(3168, 3425, 0);
    Tile FROG_KILLER_1 = new Tile(3194, 3180, 0);
    Tile IRON_ORE_1 = new Tile(3286,3368,0);
    Tile IRON_ORE_2 = new Tile(3287,3369,0);
    Tile IRON_ORE_3 = new Tile(3285,3370,0);
    private Timer timer;
    ScriptVars sv = new ScriptVars();
    //current state
    private State state;
    private GameObject currRock = null;

    private Boolean isDuckHunt = false;
    private Boolean isGuardKiller = false;
    private Boolean isGiantFrogKiller = true;
    private Boolean buryBones = true;
    private String weapon = "Maple Shortbow";

    private NPC closestNoob = null;

    private GroundItem groundItem = null;

    private Player closestPlayer = null;
    private NPC closestNPC = null;

    //private Tile startTile = null;
    private MineTask currTask = null;
    private int taskPlace = 0;
    private boolean started = false;
    private minerGui gui = null;
    //private NPC closestNPC;
    private MethodProvider mp;
    private ChatListener chatListener;
    private TestScript testScript = null;

    //private boolean isCombat = true;
    Bank bank;
    Inventory inv;
    private enum State {
        MINE, DROP, BANK, GUI, SMITH, COMBAT
    }

    private State getState() {
        if (!started) {
            return State.GUI;
        }
        //if (getLocal().isInCombat()) {
        //    return State.COMBAT;
        //}
        if (Equipment.contains(weapon) && currTask.isCombat()) {
            return State.COMBAT;
        }
        if (currTask.isPowerMine()) {
            if (Inventory.contains(currTask.getOreName())) return State.DROP;
        } else if (Inventory.isFull() && !currTask.isSmith()) {
            return State.BANK;
        } else if (Inventory.contains("Bronze bar") || Inventory.contains("Iron bar") && currTask.isSmith()) {
            return State.BANK;
        }
        if (!currTask.isSmith() && !getLocal().isInCombat()) {
            return State.MINE;
        } else {
            print("Smith");
            return State.SMITH;
        }
    }

    @Override
    public void onStart() {
        print("Starting DreamBot's AIO Mining script!");
    }

    @Override
    public int onLoop() {
        if (started) {
            if (currTask.reachedGoal()) {
                print("Finished current task!");
                taskPlace++;
                if (taskPlace >= sv.tasks.size()) {
                    print("Finished all tasks!");
                    stop();
                    return 1;
                }
                currTask = sv.tasks.get(taskPlace);
                currTask.resetTimer();
                return 200;
            }

            Player myPlayer = getLocal();
            NPC closestNPC = NPCs.closest(Character::isInCombat);
            //NPC closestNPC = NPCs.closest("Mugger");

            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Calculations.random(30, 70)) {
                Walking.toggleRun();
            }
            if (myPlayer.isMoving() && Client.getDestination() != null && Client.getDestination().distance(myPlayer) > 5) {
                //print("My player is moving");
                return Calculations.random(300, 600);
            }

            if (myPlayer.isInCombat() && Inventory.isFull() && !Inventory.contains("Trout")) {
                print("My player is in combat");
                Walking.walk(currTask.getBank().getCenter());
                return Calculations.random(300, 600);
            }

            //if (myPlayer.isInCombat() && Objects.equals(state.toString(),"MINE") && myPlayer.getHealthPercent() > 50) {
            //    print("Fighting with " + closestNPC);
            //    print("Health precent of player is " + myPlayer.getHealthPercent());
            //    return Calculations.random(1000, 3000);
            //}
        }
        state = getState();
        switch (state) {
            case GUI:
                if (gui == null) {
                    SwingUtilities.invokeLater(() -> gui = new minerGui(sv));
                    sleep(300);
                } else if (!gui.isVisible() && !sv.started) {
                    SwingUtilities.invokeLater(() -> gui.setVisible(true));
                    sleep(1000);
                } else {
                    if (!sv.started) {
                        sleep(300);
                    } else {
                        //bank = Bank;
                        //inv = Inventory;
                        SwingUtilities.invokeLater(() -> currTask = sv.tasks.get(0));
                        SwingUtilities.invokeLater(() -> currTask.resetTimer());
                        SwingUtilities.invokeLater(() -> SkillTracker.start(Skill.SMITHING));
                        SwingUtilities.invokeLater(() -> SkillTracker.start(Skill.MINING));
                        SwingUtilities.invokeLater(() -> timer = new Timer());
                        SwingUtilities.invokeLater(() -> started = true);
                    }
                }
                break;
            case BANK:
                print("Switching to case BANK");
                if (bank.isOpen()) {
                    if (inv.get(new Filter<Item>() {
                        public boolean match(Item i) {
                            if (i == null || i.getName() == null) {
                                return false;
                            }
                            return i.getName().contains("pickaxe");
                        }
                    }) != null) {
                        for (int i = 0; i < 28; i++) {
                            final Item item = inv.getItemInSlot(i);
                            if (item != null && !item.getName().contains("pickaxe")) {
                                bank.depositAll(item.getName());
                                Sleep.sleepUntil(() -> !inv.contains(item.getName()), 2000);
                            }

                        }
                    } else {
                        Bank.depositAllItems();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return Inventory.isEmpty() && getLocal().getHealthPercent() == 100;
                            }
                        }, 2000);

                    }
                } else {
                    if (currTask.getBank().getArea(Calculations.random(7,10)).contains(getLocal()))
                    {
                        Bank.open();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return Bank.isOpen();
                            }
                        }, 2000);
                    }
                    else {
                        print("Liigun panka!");
                        Walking.walk(currTask.getBank().getCenter());
                        AntiPattern();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return getLocal().isMoving();
                            }
                        }, 2000);
                    }
                }
                break;
            case DROP:
                print("Started drop");
                currRock = null;
                Item ore = inv.get(currTask.getOreName());
                if (ore != null) {
                    inv.interact(ore.getName(), "Drop");
                    Sleep.sleepUntil(new Condition() {
                        public boolean verify() {
                            Item ore = inv.get(currTask.getOreName());
                            return ore == null;
                        }
                    }, 1200);
                }
                break;
            case SMITH:
                int copperOreCount = Bank.count("Copper ore");
                int tinOreCount = Bank.count("Tin ore");
                int ironOreCount = Bank.count("Iron ore");
                if (copperOreCount > 1000 && tinOreCount > 1000) {
                    currTask.setOreName("Bronze bar");
                }
                if (ironOreCount > 100) {
                    currTask.setOreName("Iron bar");
                }
                print("Changing task to " + currTask.getOreName());
                currTask.setBank(BankLocation.EDGEVILLE);
                print("Changing bank location to " + currTask.getBank());
                if (!currTask.getGoal().contains("mine") && currTask.getOreName().equals("Bronze bar")) {
                    currTask.setGoal("mine=" + Math.min(copperOreCount, tinOreCount));
                }
                if (!currTask.getGoal().contains("mine") && currTask.getOreName().equals("Iron bar")) {
                    currTask.setGoal("mine=" + ironOreCount);
                }
                Bank.getClosestBankLocation();
                grabBronzeBarsToSmelt();
                WalkToFurnace();
                InteractWithFurnace();
                break;
            case MINE:
                //currTask.setOreName("Copper ore");
                //currTask.setBank(BankLocation.VARROCK_EAST);
                if (!Tab.INVENTORY.isOpen()) {
                    Tabs.openWithMouse(Tab.INVENTORY);
                    sleep(300, 500);
                }
                NPC strayDog = NPCs.closest("Stray dog");
                if (strayDog != null && strayDog.isOnScreen() && strayDog.isInCombat() && strayDog.canReach(getLocal().getTile())) {
                    int random = Calculations.random(1,50);
                    print("Stray dog anti pattern " + random);
                    if (random == 1) {
                        strayDog.interact("Pet");
                    }
                    if (random == 25) {
                        strayDog.interact("Shoo-away");
                    }
                    Sleep.sleepUntil(() -> !strayDog.isInCombat(), 1200);
                }
                if (Bank.isOpen()) {
                    bank.close();

                    print("Bank closed! Going to " + getState());
                    Sleep.sleepUntil(new Condition() {
                        public boolean verify() {
                            return !bank.isOpen();
                        }
                    }, 1200);
                }
                else {
                    if (Inventory.contains("Bronze pickaxe")) {
                        Inventory.interact("Bronze pickaxe", "Wield");
                    } else if  (Inventory.contains("Iron pickaxe")) {
                        Inventory.interact("Iron pickaxe", "Wield");
                    }
                    else if (currTask.getStartTile().distance(getLocal()) > 10) {
                        Walking.walk(currTask.getStartTile());
                        //AntiPattern();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return getLocal().isMoving();
                            }
                        }, 2000);
                    } else if ((currTask.dontMove() && !getLocal().getTile().equals(currTask.getStartTile()))) {
                        print("liigun");
                        Walking.walk(currTask.getStartTile());
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return getLocal().isMoving();
                            }
                        }, 2000);
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return !getLocal().isMoving();
                            }
                        }, 2000);
                    } //kui dont move on checked
                    else {
                        if (Camera.getPitch() < 270) {
                            Camera.rotateToPitch((int) (Calculations.random(300, 400) * Client.seededRandom()));
                        }
                        if (getLocal().getAnimation() == -1 && (currRock == null || !currRock.isOnScreen() || !currTask.isPowerMine())) {
                            print("Going to find " + currTask.getRock());
                            currRock = currTask.getRock();//getGameObjects().getClosest(currTask.getIDs());
                        }
                        if (getLocal().getAnimation() == -1 || (getLocal().getAnimation() == 624 && currRock == null)) {
                            print("Running mining sequence");
                            if (currRock != null && currRock.exists() && getLocal().getTile().equals(currTask.getStartTile())) {
                                currRock.interact("Mine");
                                print("Starting to mine " + currTask.getRock());
                                AntiPattern();
                                if (currRock.interact("Mine")) {
                                    print("Mining");
                                    sleep(100,300);
                                    if (currTask.isPowerMine()) {
                                        hover(true);
                                    } else {
                                        print("Current start tile is " + currTask.getStartTile());
                                        print("Current player tile is " + getLocal().getTile());
                                        if (currTask.getStartTile() != getLocal().getTile() && getLocal().getAnimation() == 1) {
                                            print("Start tile is not equal to current tile");
                                            print("Walking to start tile");
                                            Walking.walk(currTask.getStartTile());
                                        }
                                        //Sleep.sleepUntil(() -> currRock.getTile() != null, 1800);
                                        //print("Testing null");
                                        //Sleep.sleepUntil(() -> currRock == null, 200);

                                        //Sleep.sleepUntil(() -> getLocal().getAnimation() != -1, 2000);
                                        //print("Sleeping1");
                                        //Sleep.sleepUntil(() -> getLocal().getAnimation() == -1, 1800);
                                        //print("Sleeping2");
                                        //sleep(0, 100);
                                    }
                                }
                            }
                        }

                        if (currRock == null && getLocal().getAnimation() == 624) {
                            print("Testing feil2 " + getLocal().getAnimation());
                            print("Testing feil2 " + currTask.getRock());
                            currRock.interact("Mine");
                        }
                        if (getLocal().getAnimation() == -1) {
                            if (Objects.equals(currTask.getOreName(), "Tin ore")) {
                                print("Mining tin ore");
                            }
                            if (Objects.equals(currTask.getOreName(), "Copper ore")) {
                                print("Mining copper ore");
                            }
                            if (Objects.equals(currTask.getOreName(), "Iron ore")) {
                                if (currRock != null) {
                                    print("Mining " + currRock.getName() + " ore " + currRock.getTile());
                                }
                            }

                            Sleep.sleepWhile(() -> getLocal().getAnimation() == 625, 20000);
                            //AntiPattern();
                        }

                        if (getLocal().getAnimation() == -1 && !currTask.getStartTile().equals(getLocal().getTile())) {
                            Walking.walk(currTask.getStartTile());
                            print("Walking to " + currTask.getStartTile() + " tile");
                        }
                        List<Player> allPlayers = Players.all();
                        allPlayers.remove(getLocal());
                        //If other player is mining on same tile, find new one.
                        if (allPlayers.size() > 0) {
                            //Player closestPlayer = allPlayers.get(closest(0-1));
                            Player randomPlayer = allPlayers.get(Calculations.random(allPlayers.size()));
                            Tile PlayerOnSameTile = randomPlayer.getTile();
                            String PlayerName = randomPlayer.getName();


                            //If Ore name = Copper ore and current tile is occupied by someone else get new tile
                            if (PlayerOnSameTile.equals(currTask.getStartTile()) && Objects.equals(currTask.getOreName(), "Copper ore")) {
                                int randomNr = Calculations.random(1, 3);
                                print("Getting new " + currTask.getRock() + " tile " + randomNr);
                                if (randomNr == 1 && !getLocal().getTile().equals(currTask.getStartTile())) {
                                    currTask.setStarTile(COPPER_ORE_1);
                                }
                                if (randomNr == 2 && !getLocal().getTile().equals(currTask.getStartTile())) {
                                    currTask.setStarTile(COPPER_ORE_2);
                                }
                                if (randomNr == 3 && !getLocal().getTile().equals(currTask.getStartTile())) {
                                    currTask.setStarTile(COPPER_ORE_3);
                                }
                                print("player on same Tile " + PlayerOnSameTile + ' ' + PlayerName);
                            }
                            //If Ore name = Tin ore and current tile is occupied by someone else get new tile
                            if (PlayerOnSameTile.equals(currTask.getStartTile()) && Objects.equals(currTask.getOreName(), "Tin ore")) {
                                int randomNr = Calculations.random(1, 3);
                                print("Getting new " + currTask.getRock() + " tile " + randomNr);
                                if (randomNr == 1) {
                                    currTask.setStarTile(TIN_ORE_1);
                                }
                                if (randomNr == 2) {
                                    currTask.setStarTile(TIN_ORE_2);
                                }
                                if (randomNr == 3) {
                                    currTask.setStarTile(TIN_ORE_3);
                                }
                                print("player on same Tile " + PlayerOnSameTile + ' ' + PlayerName);
                            }
                        }
                        //print("player on same tile" + newTile + PlayerName);

                        sleep(100, 300);
                    }
                }
                currTask.getTracker().update();
                break;
            case COMBAT:
                WidgetChild chatMessage = Widgets.get(162, 56, 1);
                Message message = new Message(4, "", chatMessage.getText().toLowerCase(), 0);
                onGameMessage(message);
                print("testing chat " + chatMessage.getText());
                while (!Inventory.contains("Trout") && !getLocal().isInCombat()) {
                    {
                        Bank.open();
                        sleep(500, 1000);
                        Bank.depositAllItems();
                        Bank.withdraw("Trout", 26);
                        Bank.close();
                        sleep(500,1000);
                        Sleep.sleepUntil(() -> getLocal().getHealthPercent() > 90, 2000);
                    }
                }
                    if (getLocal().getHealthPercent() < 70) {
                        Inventory.interact("Trout");
                    }
                    //Always sets camera behind player
                    if (getLocal().getOrientation() != Camera.getYaw()) {
                        int random = Calculations.random(0,10);
                        if (random == 1) {
                            Camera.rotateToYawEvent((getLocal().getOrientation() - 1024) * -1);
                        }
                    }
                    if (Camera.getPitch() < 200) {
                        Camera.rotateToPitchEvent(Calculations.random(230, 300));
                        Camera.setZoom(Calculations.random(400, 500));
                    }
                    if (isDuckHunt) {
                        currTask.setStarTile(DUCK_HUNTER_1);
                        closestNoob = NPCs.closest("Duck");
                        //Boolean playerAttackedBy = getLocal().getCharacterInteractingWithMe().getName().equals("Duck");

                        if (!getLocal().isInCombat() && !closestNoob.isInCombat() && closestNoob != null) {
                            if (closestNoob.distance(getLocal()) < 10) {
                                closestNoob.interact("Attack");
                                print("Testing " + closestNoob.canReach(getLocal().getTile()));
                            }
                        }
                        sleep(300, 600);
                        if (!getLocal().isInCombat() && closestNoob.isInCombat() && closestNoob != null) {
                            closestNoob.interact("Attack");
                        }
                        if (!currTask.getStartTile().getArea(Calculations.random(7, 10)).contains(getLocal()) && !getLocal().isInCombat()) {
                            Walking.walk(currTask.getStartTile());
                            print("Player is too far. Returning to start tile for duck hunting");
                        }
                        sleep(100, 300);
                        print("Testing " + closestNoob.canReach(getLocal().getTile()));
                        print("Testing new " + (closestNoob.distance(getLocal().getTile()) < 10));
                        print("Attacking " + closestNoob);
                        AntiPattern();
                        print("Healt is at " + getLocal().getHealthPercent() + "%");
                    }
                    if (isGuardKiller) {
                        currTask.setStarTile(GUARD_KILLER_1);
                        closestNoob = NPCs.closest("Guard");
                        groundItem = GroundItems.closest("Adamant arrow", "Bones", "Coins", "Nature rune");
                    }
                    if (isGiantFrogKiller) {
                        currTask.setStarTile(FROG_KILLER_1);
                        closestNoob = NPCs.closest(frogFilter);
                        groundItem = GroundItems.closest(bigBones);
                        closestPlayer = Players.closest(checkAreaForPlayer);
                        closestNPC = NPCs.closest(filterNPCsWhoAreCloseToOtherPlayers);
                    }
                    if (closestPlayer != null) {
                        print("Player close " + closestPlayer);
                        print("NPC " + closestNPC + " is out of range from " + closestPlayer);
                    }
                    //if (closestPlayer != null) {
                    //    int random = Calculations.random(1, 10);
                    //    if (random == 1) {
                    //        WorldHopper.quickHop(134);
                    //    }
                    //    if (random == 2) {
                    //        WorldHopper.quickHop(136);
                    //    }
                    //}
                NPC isAttackingPlayer = NPCs.closest(attackingPlayer);
                    if (buryBones) {
                        if (Inventory.count("Big bones") > Calculations.random(1, 10) && getLocal().getInteractingIndex() == -1) {
                            while (Inventory.count("Big bones") > 0) {
                                Inventory.interact("Big bones");
                                Sleep.sleepUntil(() -> !Inventory.contains("Big bones"), 1000);
                            }
                            Sleep.sleepUntil(() -> !Inventory.contains("Big bones"), 1000);
                        }
                    }
                    if (groundItem != null && !Inventory.isFull() && getLocal().getInteractingIndex() == -1) {
                    groundItem.interact("Take");
                    print("Picking up " + groundItem.getName());
                    //Sleep.sleepUntil(() -> !getLocal().isMoving(), 1000);
                    if (groundItem != null && Inventory.count("Adamant arrow") > Calculations.random(1, 100)) {
                        Inventory.interact("Adamant Arrow");
                    }
                }
                    //Attacking Giant frogs that are not in combat
                if (!getLocal().isInCombat() && closestNoob != null && closestNoob.getIndex() != 1137 && groundItem == null) {
                        if (closestNoob.distance(getLocal()) < 10) {
                            //closestNoob = NPCs.closest(frogFilter);
                            closestNoob.interact("Attack");
                            print("Testing " + closestNoob.canReach(getLocal().getTile()));
                            print("Testing new" + frogFilter);
                        }
                    }
                if (Equipment.contains(weapon) && isGuardKiller) {
                    test(GUARD_KILLER_2);
                    test(GUARD_KILLER_1);
                }
                //Needs work
                if (!currTask.getStartTile().getArea(Calculations.random(10, 15)).contains(getLocal()) && !getLocal().isInCombat() && groundItem == null) {
                    Walking.walk(currTask.getStartTile());
                    print("Player is too far. Returning to start tile for duck hunting");
                }
                if (groundItem != null) {
                    print("Testing distance " + groundItem.getTile().distance(getLocal()));
                }
                AntiPattern();
                print("Healt is at " + getLocal().getHealthPercent() + "%");
                //print("Frog is in combat? " + closestNoob.isInCombat());
                //print("Frog interacting with player? " + closestNoob.isInteracting(getLocal()));
                //closestNoob = NPCs.closest(attackingPlayer);
                print("Correct frog is interacting with player? " + closestNoob);
                    while (Dialogues.canContinue()) {
                    Dialogues.spaceToContinue();
                    sleep(1000, 1500);
                    }
                    sleep(300, 600);
                break;
        }
        return 200;
    }

    private void test(Tile guardKiller2) {
        if ((closestNoob.getAnimation() != -1 || closestNoob.getAnimation() != 1156) && getLocal().isHealthBarVisible() && closestNoob.canAttack()) {
            print(closestNoob + " is too close");
            currTask.setStarTile(guardKiller2);
            print("Setting new tile to " + guardKiller2);
            Walking.walk(guardKiller2);
            print(("Walking to " + guardKiller2));
            sleep(500,1000);
            if (getLocal().getTile().equals(guardKiller2)) {
                closestNoob.interact("Attack");
            }
            Sleep.sleepUntil(() -> getLocal().getTile().equals(guardKiller2), 1000);
            if (getLocal().getTile().equals(guardKiller2)) {
                closestNoob.interact("Attack");
            }
            //Sleep.sleepUntil(() -> getLocal().getTile().equals(guardKiller2),
            Sleep.sleepUntil(() -> !getLocal().isHealthBarVisible() && !getLocal().getTile().equals(guardKiller2), 7000);
        }
    }

    private Tile getRandomCopperOreTile() {
        //If Ore name = Copper ore and current tile is occupied by someone else get new tile
        if (Objects.equals(currTask.getOreName(), "Copper ore")) {
            int randomNr = Calculations.random(1, 3);
            print("Getting new tile" + randomNr);
            if (randomNr == 1) {
                currTask.setStarTile(COPPER_ORE_1);
            }
            if (randomNr == 2) {
                currTask.setStarTile(COPPER_ORE_2);
            }
            if (randomNr == 3) {
                currTask.setStarTile(COPPER_ORE_3);
            }
        } return currTask.getStartTile();
    }

    private Tile getRandomTinOreTile() {
            if (Objects.equals(currTask.getOreName(), "Tin ore")) {
                int randomNr = Calculations.random(1, 3);
                print("Getting new tile" + randomNr);
                //If Ore name = Tin ore and current tile is occupied by someone else get new tile
                if (randomNr == 1) {
                    currTask.setStarTile(TIN_ORE_1);
                }
                if (randomNr == 2) {
                    currTask.setStarTile(TIN_ORE_2);
                }
                if (randomNr == 3) {
                    currTask.setStarTile(TIN_ORE_3);
                }
            } return currTask.getStartTile();
        }
        /*
        else if (Objects.equals(currTask.getOreName(), "Iron ore")) {
            int randomNr = Calculations.random(1,3);
            print("Getting new tile" + randomNr);
            //If Ore name = Tin ore and current tile is occupied by someone else get new tile
            if (randomNr == 1) {
                currTask.setStarTile(IRON_ORE_1);
            }
            if (randomNr == 2) {
                currTask.setStarTile(IRON_ORE_2);
            }
            if (randomNr == 3) {
                currTask.setStarTile(IRON_ORE_3);
            }
        }*/
    private Inventory getInventory() {
        return inv;
    }
    private void botMessage(Message message){
        if (message.getMessage().matches("bot")) {
            Keyboard.type("Yeap");
        }
    }

    public void grabBronzeBarsToSmelt() {

        print("If bank is not open, open bank");
        if (!Bank.isOpen()) {
            Bank.open();
        } Sleep.sleepUntil(new Condition() {
            public boolean verify() {
                return Bank.isOpen();
            }
        }, 1200);

        if (Objects.equals(currTask.getOreName(), "Iron bar") && Bank.contains("Iron ore")) {
            if (Bank.count("Iron ore") < 1) {
                print("not enough ore");
                Bank.getClosestBankLocation();
                Sleep.sleepUntil(new Condition() {
                    public boolean verify() {
                        return !Bank.isOpen();
                    }
                }, 5000);
                Bank.depositAllItems();
                print("Getting copper and tin ore to smelth");
                currTask.setOreName("Copper ore");
            }
            else {
                if (Inventory.contains("Copper ore") && Inventory.contains("Tin ore")) {
                    InteractWithFurnace();
                }
                if (!Bank.isOpen()) {
                    print("open bank");
                    Bank.open();
                    sleep(500, 1200);
                    //checking if there are enough ores in the bank, if not, then stopping the script
                    if (!Inventory.isEmpty()) {
                        AFK();
                        Bank.depositAllItems();
                        sleep(300, 1000);
                    }
                }
                if (Bank.isOpen() && Inventory.isEmpty()) {
                    Bank.withdraw("Iron ore", 28);
                    Mouse.move();
                    sleep(100, 500);
                }
                while (!bank.isOpen()) {
                    bank.getClosestBankLocation();
                    Mouse.move();
                    sleep(1500, 3000);
                }
                while (bank.isOpen()) {

                    bank.close();
                    Mouse.move();
                    sleep(100, 500);

                }
            }
        }
        else if (Objects.equals(currTask.getOreName(), "Bronze bar")) {
            if (Bank.count("Copper ore") < 14 || Bank.count("Tin ore") < 14) {
                print("not enough ore");
                Bank.getClosestBankLocation();
                Sleep.sleepUntil(new Condition() {
                    public boolean verify() {
                        return !Bank.isOpen();
                    }
                }, 5000);
                Bank.depositAllItems();
                print("Getting copper and tin ore to smelth");
                currTask.setOreName("Copper ore");
            }
            else {
                if (Inventory.contains("Copper ore") && Inventory.contains("Tin ore")) {
                    InteractWithFurnace();
                }
                if (!Bank.isOpen()) {
                    print("open bank");
                    Bank.open();
                    sleep(500, 1200);
                    //checking if there are enough ores in the bank, if not, then stopping the script
                    if (!Inventory.isEmpty()) {
                        AFK();
                        Bank.depositAllItems();
                        sleep(300, 1000);
                    }
                }
                if (Bank.isOpen() && Inventory.isEmpty()) {
                    Bank.withdraw("Tin ore", 14);
                    Mouse.move();
                    sleep(100, 500);
                    Bank.withdraw("Copper ore", 14);
                    Mouse.move();
                    sleep(300, 1000);
                }
                while (!bank.isOpen()) {
                    bank.getClosestBankLocation();
                    Mouse.move();
                    sleep(1500, 3000);
                }
                while (bank.isOpen()) {

                    bank.close();
                    Mouse.move();
                    sleep(100, 500);

                }
            }
        } else currTask.setFinished(true);
        print("Done smithing");
    }

    public void WalkToBank() {
        log("While im not in bank, walking to bank...");

        while (!EDGE_BANK.contains(getLocal())) {
            Walking.walk(EDGE_BANK.getRandomTile());
            sleep(1000, 1500);
            Camera.rotateToPitch((int) (Calculations.random(300, 400) * Client.seededRandom()));
            Camera.rotateToYaw((int) (Calculations.random(931, 1100) * Client.seededRandom()));
            Sleep.sleepUntil(() -> Walking.getDestinationDistance() < 4, 5000);
        }
    }

    public void WalkToFurnace() {
        if (!Inventory.contains("ore")) {
            Bank.getClosestBankLocation();
        }
        log("while im not at furnace.. walking to furnace...");

        Camera.rotateToPitch((int) (Calculations.random(280, 350) * Client.seededRandom()));
        Camera.rotateToYaw((int) (Calculations.random(1400, 1500) * Client.seededRandom()));
        while (!FURNACE.contains(getLocal())) {
            Walking.walk(FURNACE.getRandomTile());
            int randomnr = Calculations.random(1,10);
            print(randomnr);
            AntiPattern();
            sleep(1000, 1500);
            Sleep.sleepUntil(() -> Walking.getDestinationDistance() < Calculations.random(6,10), 2000);
        }
        Sleep.sleepUntil(new Condition() {
            public boolean verify() {
                return getLocal().isMoving();
            }
        }, 2000);

    }

    public void InteractWithFurnace() {
        print("interacting with furnace");
        while (Inventory.contains("Iron ore")) { //while loop is needed incase player levels up while smelting
            GameObject Furnace = GameObjects.closest("Furnace");
            if (Furnace != null && Furnace.hasAction("Smelt")) {
                Furnace.interact("Smelt");
                Mouse.move();
            }
            sleep(1000, 2000);
            WidgetChild ClickBronzeBar = Widgets.getWidget(270).getChild(15);//gets the bronze bar widget
            sleep(500, 1500);
            if (ClickBronzeBar != null) {
                log("Clicking Iron bar to smelt all");
                ClickBronzeBar.interact();
                currTask.getTracker().update();
            }

            AntiPattern();

            Mouse.moveOutsideScreen();
            log("Moving mouse off screen while smelting");

            Sleep.sleepUntil(() -> Inventory.count("Iron ore") < 1 || Dialogues.canContinue(), 60000);
            while (Dialogues.canContinue()) {
                Dialogues.spaceToContinue();
                sleep(1000, 1500);
            }//this is called to get past the level up message and begin smelting bronze bars again


        }
        while (Inventory.contains("Copper ore") && Inventory.contains("Tin ore")) { //while loop is needed incase player levels up while smelting
            GameObject Furnace = GameObjects.closest("Furnace");
            if (Furnace != null && Furnace.hasAction("Smelt")) {
                Furnace.interact("Smelt");
                Mouse.move();
            }
            sleep(1000, 2000);
            WidgetChild ClickBronzeBar = Widgets.getWidget(270).getChild(14);//gets the bronze bar widget
            sleep(500, 1500);
            if (ClickBronzeBar != null) {
                log("Clicking Bronze bar to smelt all");
                ClickBronzeBar.interact();

            }
            AntiPattern();
            Mouse.moveOutsideScreen();
            log("Moving mouse off screen while smelting");
            Sleep.sleepUntil(() -> Inventory.count("Bronze bar") == 14 || Dialogues.canContinue(), 60000);
            while (Dialogues.canContinue()) {
                Dialogues.spaceToContinue();
                sleep(1000, 1500);
            }//this is called to get past the level up message and begin smelting bronze bars again


        }

    }

    private Tab RandomTab() {
        //this gets method called to select a random tab in the AntiPattern method.

        int randNumber = Calculations.random(1, 7);

        if (randNumber == 1) {
            return Tab.COMBAT;
        }
        if (randNumber == 2) {
            return Tab.SKILLS;
        }
        if (randNumber == 3) {
            return Tab.QUEST;
        }
        if (randNumber == 4) {
            return Tab.EQUIPMENT;
        }
        if (randNumber == 5) {
            return Tab.PRAYER;
        }
        if (randNumber == 6) {
            return Tab.MAGIC;
        } else return null;

    }

    private void AntiPattern() {
        int random = Calculations.random(1,250);
        print("Running anti pattern sequence:" + random);
        if (random == 10) {

            log("Right clicking random player if there are any nearby");

            List<Player> allNearbyPlayers = Players.all(); // gets all nearby players and stores it into a list
            allNearbyPlayers.remove(getLocal()); //removes local player from list

            if (allNearbyPlayers.size() > 0) // checks if the list contains more than 0 players
            {
                Player randomPlayer = allNearbyPlayers.get(Calculations.random(allNearbyPlayers.size())); // get one name out of the list at random and put it into another variable called randomPlayer
                EntityDestination playerDestination = new EntityDestination(randomPlayer); //creates a destination in game for randomPlayer
                Mouse.move(playerDestination); //moves mouse over the randomplayer
                Mouse.click(ClickMode.RIGHT_CLICK); //right clicks to open menu
                sleep(3000, 6000);

            }

        }
        if (random == 114 && !getLocal().isInCombat()) {
            log("Going AFK for 60-90 seconds");
            Mouse.moveOutsideScreen();
            sleep(Calculations.random(60000, 90000)); // afk for 60-90 seconds
        }
        if (random == 76) {
            if (Objects.equals(state.toString(), "MINE")) {
                print("Checking Mining skill");
                int randLoop = Calculations.random(0, 1);
                int x = 0;

                while (x <= randLoop) {
                    x++;


                    Tabs.openWithMouse(Tab.SKILLS);
                    sleep(300, 500);
                    Skills.hoverSkill(Skill.MINING);
                    sleep(2000, 4000);
                    Tabs.openWithMouse(Tab.INVENTORY);
                    sleep(100, 300);
                }
            }
            else if (Objects.equals(state.toString(), "SMITH")) {
                print("Checking Smithing skill");
                int randLoop = Calculations.random(0, 1);
                int x = 0;

                while (x <= randLoop) {
                    x++;


                    Tabs.openWithMouse(Tab.SKILLS);
                    sleep(300, 500);
                    Skills.hoverSkill(Skill.SMITHING);
                    sleep(2000, 4000);
                    Tabs.openWithMouse(Tab.INVENTORY);
                    sleep(100, 300);
                }
            }
            else if (Objects.equals(state.toString(), "BANK")) {
                print("Checking Mining skill");
                int randLoop = Calculations.random(0, 1);
                int x = 0;

                while (x <= randLoop) {
                    x++;


                    Tabs.openWithMouse(Tab.SKILLS);
                    sleep(300, 500);
                    Skills.hoverSkill(Skill.MINING);
                    sleep(2000, 4000);
                    Tabs.openWithMouse(Tab.INVENTORY);
                    sleep(100, 300);
                }
            }
            else if (Objects.equals(state.toString(), "COMBAT") && getLocal().getEquipment().contains("bow")) {
                print("Checking Ranged skill");
                int randLoop = Calculations.random(0, 1);
                int x = 0;

                while (x <= randLoop) {
                    x++;


                    Tabs.openWithMouse(Tab.SKILLS);
                    sleep(300, 500);
                    Skills.hoverSkill(Skill.RANGED);
                    sleep(2000, 4000);
                    Tabs.openWithMouse(Tab.INVENTORY);
                    sleep(100, 300);
                }
            }
        }
        if (currTask.getBank().equals(BankLocation.VARROCK_EAST)) {
            if (random == 125 && Objects.equals(state.toString(), "MINE") && getLocal().getAnimation() == -1) {
                print("Switching to copper ore");
                currTask.setOreName("Copper ore");
                currTask.setIDs(new int[]{11161,10943});
                currTask.setStarTile(getRandomCopperOreTile());
                currTask.setBank(BankLocation.VARROCK_EAST);
                print("Setting new tile for copper ore " + currTask.getStartTile());
            }
            if (random == 126 && Objects.equals(state.toString(), "MINE")  && getLocal().getAnimation() == -1) {
                print("Switching to Tin ore");
                currTask.setOreName("Tin ore");
                currTask.setIDs(new int[]{11361,11360});
                currTask.setStarTile(getRandomTinOreTile());
                currTask.setBank(BankLocation.VARROCK_EAST);
                print("Setting new tile for tin ore " + currTask.getStartTile());
            }
        }
        //if (random == 100) {
        //    Camera.rotateToPitchEvent((int) (Calculations.random(250, 400) * Client.seededRandom()));
        //}
        //if (random == 105) {
        //    Camera.rotateToYawEvent((int) (Calculations.random(500, 600) * Client.seededRandom()));
        //}
        //if (random == 123) {
        //    Camera.rotateToYawEvent((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        //}
        //if (random == 15) {
        //    Camera.rotateToYawEvent((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        //}
        //if (random == 143) {
        //    Camera.rotateToYawEvent((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        //}
        //if (random == 221) {
        //    Camera.rotateToYawEvent((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        //}
        if (random == 111) {
            NPC closestNPCs = NPCs.closest("Mugger");
            if(closestNPCs != null && closestNPCs.isOnScreen() && getLocal().getHealthPercent() > 70) {
                closestNPCs.interact("Attack");
                combat();
            }
        }
    }
    private State combat() {
        return State.COMBAT;
    }

    private void onGameMessage(Message message) {
        if (message.getMessage().contains("bot")) {
            int random = Calculations.random(1,3); {
                if (random == 1) {
                    Keyboard.type("?");
                }
                if (random == 2) {
                    Keyboard.type("Yes");
                }
                if (random == 3) {
                    Keyboard.type("Yeap");
                }
            }
        }
    }

    private void AFK() {

        int random = Calculations.random(1, 20);

        if (random == 10) {
            log("Going AFK for 60-90 seconds");
            Mouse.moveOutsideScreen();
            sleep(Calculations.random(60000, 90000)); // afk for 60-90 seconds
        }
    }

    public int getFirstEmptySlot() {
        for (int i = 0; i < 28; i++) {
            Item it = getInventory().getItemInSlot(i);
            if (it == null || it.getName().contains("ore")) {
                return i;
            }
        }
        return 0;
    }

    public void hover(boolean fromInteract) {
        int firstEmpty = getFirstEmptySlot();
        Rectangle r = getInventory().slotBounds(firstEmpty);
        if (!r.contains(Mouse.getPosition())) {
            int x1 = (int) r.getCenterX() - Calculations.random(0, 10);
            int y1 = (int) r.getCenterY() - Calculations.random(0, 10);
            int x2 = (int) r.getCenterX() + Calculations.random(0, 10);
            int y2 = (int) r.getCenterY() + Calculations.random(0, 10);
            int fX = Calculations.random(x1, x2);
            int fY = Calculations.random(y1, y2);
            Mouse.move(new Point(fX, fY));
        }
        if (fromInteract) {
            Sleep.sleepUntil(new Condition() {
                public boolean verify() {
                    return getLocal().getAnimation() != -1;
                }
            }, 2000);
        }
    }

    @Override
    public void onExit() {
        gui.setVisible(false);
        log("Stopping testing!");
    }

    public void onPaint(Graphics g) {
        if (started) {
            g.setColor(Color.green);
            if (state != null) g.drawString("State: " + state.toString(), 5, 50);
            g.drawString("Total Runtime: " + timer.formatTime(), 5, 65);
            g.drawString("Task Runtime: " + currTask.getTimer().formatTime(), 5, 80);
            g.drawString("MINING EXP", 180, 75);
            g.drawString("Experience(p/h): " + SkillTracker.getGainedExperience(Skill.MINING) + "(" + SkillTracker.getGainedExperiencePerHour(Skill.MINING) + ")", 180, 95);
            g.drawString("Level(gained): " + Skills.getRealLevel(Skill.MINING) + "(" + SkillTracker.getGainedLevels(Skill.MINING) + ")", 180, 110);
            g.drawString("Experience(p/h): " + SkillTracker.getGainedExperience(Skill.SMITHING) + "(" + SkillTracker.getGainedExperiencePerHour(Skill.SMITHING) + ")", 5, 95);
            g.drawString("Level(gained): " + Skills.getRealLevel(Skill.SMITHING) + "(" + SkillTracker.getGainedLevels(Skill.SMITHING) + ")", 5, 110);
            g.drawString("Ores(p/h): " + currTask.getTracker().getAmount() + "(" + timer.getHourlyRate(currTask.getTracker().getAmount()) + ")", 10, 125);
            g.drawString("Current task: " + currTask.getOreName() + "::" + currTask.getGoal(), 10, 140);
            for (int i = 0; i < sv.tasks.size(); i++) {
                MineTask mt = sv.tasks.get(i);
                if (mt != null) {
                    if (mt.getFinished()) {
                        g.setColor(Color.blue);
                    } else {
                        g.setColor(Color.red);
                    }
                    String task = mt.getOreName() + "::" + mt.getGoal();
                    g.drawString(task, 10, 155 + i * 15);
                }
            }
        } else {
            List<GameObject> rocks = GameObjects.all(new Filter<GameObject>() {
                public boolean match(GameObject go) {
                    if (go == null || !go.exists() || go.getName() == null || !(go.getName().length() > 6))
                        return false;
                    if (!go.isOnScreen()) return false;
                    return true;
                }
            });
            if (!rocks.isEmpty()) {
                for (GameObject go : rocks) {
                    Tile rockTile = go.getTile();
                    Rectangle tileRect = Map.getBounds(rockTile);
                    Point startPoint = new Point((int) tileRect.x, (int) tileRect.getCenterY());
                    g.drawString("ID: " + go.getID(), startPoint.x, startPoint.y);
                }
            }
        }
    }

    public boolean walkOnScreen(Tile t) {
        Mouse.move(Client.getViewportTools().tileToScreen(t));
        String action = Menu.getDefaultAction();
        if (action != null && action.equals("Walk here")) {
            return Mouse.click();
        } else {
            Mouse.click(true);
            Sleep.sleepUntil(new Condition() {
                public boolean verify() {
                    return Menu.isMenuManipulationActive();
                }
            }, 600);
            return Menu.clickAction("Walk here");
        }
    }
    private final Filter<NPC> frogFilter = go -> !go.isInCombat() && go.getName().equals("Giant frog");

    private final Filter<GroundItem> bigBones = gi -> (gi.getName().equals("Big bones") || gi.getName().equals("Adamant arrow")) && gi.getTile().distance(getLocal()) < 10;

    private final Filter<NPC> attackingPlayer = go -> go.isInteracting(getLocal());

    private final Filter<Player> checkAreaForPlayer = go -> go.getTile().distance(getLocal()) < 10 && !go.getName().equals(getLocal().getName());

    private final Filter<NPC> checkAreaForClosestNPC = go -> go.getTile().distance(getLocal()) < 1;

    private final Filter<NPC> filterNPCsWhoAreCloseToOtherPlayers = go -> go.getTile().distance(closestPlayer) > 10;
    private NPC getNPCnotInCombat(){
       List<NPC> acceptableNPCs = NPCs.all(frogFilter);
        return (NPC) acceptableNPCs;
    }
    //private NPC getClosest(List<NPC> closestNew){
    //    NPC closestNewb = null;
    //    return closestNewb;
    //}
}
