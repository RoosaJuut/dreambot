package roosajuut.dreambot.scriptmain.powerminer;

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

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static org.dreambot.api.methods.interactive.Players.*;

@ScriptManifest(author = "Hents", description = "Power Miner/Smelter", name = "DreamBot Power Miner", version = 1.81, category = Category.MINING)
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
    Tile IRON_ORE_1 = new Tile(3286,3368,0);
    Tile IRON_ORE_2 = new Tile(3287,3369,0);
    Tile IRON_ORE_3 = new Tile(3285,3370,0);
    private Timer timer;
    ScriptVars sv = new ScriptVars();
    //current state
    private State state;
    private GameObject currRock = null;
    //private Tile startTile = null;
    private MineTask currTask = null;
    private int taskPlace = 0;
    private boolean started = false;
    private minerGui gui = null;

    private MethodProvider mp;
    Bank bank;
    Inventory inv;


    private enum State {
        MINE, DROP, BANK, GUI, SMITH
    }

    private State getState() {
        if (!started) {
            return State.GUI;
        }
        if (currTask.isPowerMine()) {
            if (Inventory.contains(currTask.getOreName())) return State.DROP;
        } else if (Inventory.isFull() && !currTask.isSmith()) {
            return State.BANK;
        } else if (Inventory.contains("Bronze bar") || Inventory.contains("Iron bar") && currTask.isSmith()) {
            return State.BANK;
        }
        if (!currTask.isSmith()) {
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
            if (!Walking.isRunEnabled() && Walking.getRunEnergy() > Calculations.random(30, 70)) {
                Walking.toggleRun();
            }
            if (myPlayer.isMoving() && Client.getDestination() != null && Client.getDestination().distance(myPlayer) > 5) {
                //print("My player is moving");
                return Calculations.random(300, 600);
            }

            if (myPlayer.isInCombat() && Inventory.isFull()) {
                print("My player is in combat");
                Walking.walk(currTask.getBank().getCenter());
                return Calculations.random(300, 600);
            }



        }
        state = getState();
        switch (state) {
            case GUI:
                if (gui == null) {
                    gui = new minerGui(sv);
                    sleep(300);
                } else if (!gui.isVisible() && !sv.started) {
                    gui.setVisible(true);
                    sleep(1000);
                } else {
                    if (!sv.started) {
                        sleep(300);
                    } else {
                        //bank = Bank;
                        //inv = Inventory;
                        currTask = sv.tasks.get(0);
                        currTask.resetTimer();
                        SkillTracker.start(Skill.SMITHING);
                        SkillTracker.start(Skill.MINING);
                        timer = new Timer();
                        started = true;
                    }
                }
                break;
            case BANK:

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
                                Sleep.sleepUntil(new Condition() {
                                    public boolean verify() {
                                        return !inv.contains(item.getName());
                                    }
                                }, 2000);
                            }
                        }
                    } else {
                        Bank.depositAllItems();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return Inventory.isEmpty();
                            }
                        }, 2000);
                    }
                } else {
                    if (currTask.getBank().getArea(Calculations.random(7,10)).contains(getLocal())) {
                        Bank.open();
                        Sleep.sleepUntil(new Condition() {
                            public boolean verify() {
                                return Bank.isOpen();
                            }
                        }, 2000);
                    } else {
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

                grabBronzeBarsToSmelt();
                WalkToFurnace();
                InteractWithFurnace();
                break;
            case MINE:
                if (Bank.isOpen()) {
                    bank.close();

                    print("Bank closed! Going to mine");
                    Sleep.sleepUntil(new Condition() {
                        public boolean verify() {
                            return !bank.isOpen();
                        }
                    }, 1200);
                } else {
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
                        if (getLocal().getAnimation() == -1 && (currRock == null || !currRock.isOnScreen() || !currTask.isPowerMine()))
                            print("Going to find " + currTask.getRock());
                            currRock = currTask.getRock();//getGameObjects().getClosest(currTask.getIDs());
                        if (getLocal().getAnimation() == -1) {
                            print("Running mining sequence");
                            if (currRock != null && currRock.exists() && getLocal().getTile().equals(currTask.getStartTile())) {
                                currRock.interact("Mine");
                                print("Starting to mine " + currTask.getRock());
                                AntiPattern();
                                if (currRock.interact("Mine")) {
                                    print("Mining");
                                    sleep(300,3000);
                                    if (currTask.isPowerMine()) {
                                        hover(true);
                                    } else {
                                        print("Current start tile is " + currTask.getStartTile());
                                        print("Current player tile is " + getLocal().getTile());
                                        if (currTask.getStartTile() != getLocal().getTile() && getLocal().getAnimation() != 625) {
                                            print("Start tile is not equal to current tile");
                                            print("Walking to start tile");
                                            Walking.walk(currTask.getStartTile());
                                        }

                                        Sleep.sleepUntil(new Condition() {

                                            public boolean verify() {
                                                return getLocal().getAnimation() != -1;
                                            }
                                        }, 2000);

                                        Sleep.sleepUntil(new Condition() {
                                            public boolean verify() {
                                                return getLocal().getAnimation() == -1;
                                            }
                                        }, 1800);
                                        sleep(300, 500);
                                    }
                                }
                            }
                        }

                        if (getLocal().getAnimation() == -1) {
                            if (Objects.equals(currTask.getOreName(), "Tin ore")) {
                                print("Mining tin ore");
                            }
                            if (Objects.equals(currTask.getOreName(), "Copper ore")) {
                                print("Mining copper ore");
                            }
                            Sleep.sleepWhile(new Condition() {
                                public boolean verify() {
                                    return getLocal().getAnimation() == 625;
                                }
                            }, 20000);
                            //AntiPattern();
                        } if (getLocal().getAnimation() == -1 && !currTask.getStartTile().equals(getLocal().getTile())) {
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

                        sleep(300, 1500);
                    }
                }
                currTask.getTracker().update();
                break;
        }
        return 200;
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
            print("getting iron ores to smelt");
            if (Bank.count("Iron ore") < 1) {
                print("not enough ore");
                Bank.depositAllItems();
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
                } else {
                    Bank.withdraw("Iron ore", 28);
                    Mouse.move();
                    sleep(300, 1000);
                }
            }
            if (Bank.isOpen() && Inventory.isEmpty()) {
                Bank.withdraw("Iron ore", 28);
                Mouse.move();
                sleep(300, 1000);
            }
        }
        else if (Objects.equals(currTask.getOreName(), "Bronze bar")) {
            if (Bank.count("Copper ore") < 14 || Bank.count("Tin ore") < 14) {
                print("not enough ore");
                stop();
                Bank.getClosestBankLocation();
                Sleep.sleepUntil(new Condition() {
                    public boolean verify() {
                        return !Bank.isOpen();
                    }
                }, 5000);
                Bank.depositAllItems();
                print("getting copper and tin ore to smelth");
            } else {
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
            } else if (Objects.equals(state.toString(), "SMITH")) {
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
        }
        if (random == 125 && Objects.equals(state.toString(), "MINE") && getLocal().getAnimation() == -1) {
            print("Switching to copper ore");
            currTask.setOreName("Copper ore");
            currTask.setIDs(new int[]{11161,10943});
            currTask.setStarTile(getRandomCopperOreTile());
            print("Setting new tile for copper ore " + currTask.getStartTile());
        }
        if (random == 126 && Objects.equals(state.toString(), "MINE")  && getLocal().getAnimation() == -1) {
            print("Switching to Tin ore");
            currTask.setOreName("Tin ore");
            currTask.setIDs(new int[]{11361,11360});
            currTask.setStarTile(getRandomTinOreTile());
            print("Setting new tile for tin ore " + currTask.getStartTile());
        }
        /*
        if (random == 46 && Objects.equals(state.toString(), "MINE")){
            print("Switching to Iron ore");
            currTask.setOreName("Iron ore");
            currTask.setIDs(new int[]{11365,11364});
            currTask.setStarTile(getRandomTile());
        }*/
        if (random == 100) {
            Camera.rotateToPitch((int) (Calculations.random(250, 400) * Client.seededRandom()));
        }
        if (random == 105) {
            Camera.rotateToYaw((int) (Calculations.random(500, 600) * Client.seededRandom()));
        }
        if (random == 123) {
            Camera.rotateToYaw((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        }
        if (random == 15) {
            Camera.rotateToYaw((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        }
        if (random == 143) {
            Camera.rotateToYaw((int) (Calculations.random(100, 1600) * Client.seededRandom()));
        }
        if (random == 221) {
            Camera.rotateToYaw((int) (Calculations.random(100, 1600) * Client.seededRandom()));
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
                    if (go == null || !go.exists() || go.getName() == null || !go.getName().equals("Rocks"))
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

}
