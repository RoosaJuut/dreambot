package roosajuut.dreambot.scriptmain.powerminer;

import org.dreambot.api.utilities.Logger;
import roosajuut.dreambot.tools.PricedItem;
import roosajuut.dreambot.scriptmain.powerminer.Miner;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;

import java.util.Arrays;
import java.util.List;

public class MineTask {
	private Ores ores;
	private String goal = "";
	private Tile startTile = null;
	private int[] ids;
	private boolean powermine = false;
	private boolean smith = false;
	private Timer t;
	private String oreName = "";
	private PricedItem oreTracker;
	private BankLocation bank;
	private boolean finished = false;
	private boolean dontMove = false;
	private MineTask currTask = null;
	public void print(Object msg) {
		Logger.log(msg);
	}

	private final Filter<GameObject> rockFilter = go -> {
		if(go == null || !go.exists() || go.getName() == null || !(go.getName().length() > 6))
			return false;
		boolean hasID = false;
		for(int i = 0; i < getIDs().length; i++){
			if(go.getID() == getIDs()[i]){
				hasID = true;
				break;
			}
		}
		if(!hasID)
			return false;
		if(dontMove() && go.distance(Players.getLocal()) > 1)
			return false;
		return true;
	};

	public MineTask(String oreName, int[] ids, Tile startTile, String goal, boolean powermine, boolean smith, BankLocation bank, boolean dontMove, Ores ores){
		this.oreName = oreName;
		this.ids = ids;
		this.startTile = startTile;
		this.goal = goal;
		this.powermine = powermine;
		this.smith = smith;
		this.oreTracker = new PricedItem(oreName, false);
		this.bank = bank;
		this.dontMove = dontMove;
		this.ores = ores;
		t = new Timer();
	}
	
	public boolean dontMove(){
		return this.dontMove;
	}
	
	public void resetTimer(){
		t = new Timer();
	}
	
	public boolean reachedGoal(){
		if(goal.toLowerCase().contains("bank")){
			Item ore = Bank.get(oreName);
			if(ore == null)
				return false;
			else{
				if(ore.getAmount() >= Integer.parseInt(goal.split("=")[1])){
					this.finished = true;
					return true;
				}
				return false;
			}
		}
		else if(goal.toLowerCase().contains("level")){
			this.finished =  Skills.getRealLevel(Skill.MINING) >= Integer.parseInt(goal.split("=")[1]);
			return finished;
		}
		else if(goal.toLowerCase().contains("mine")){
			this.finished = oreTracker.getAmount() >= Integer.parseInt(goal.split("=")[1]);
			return finished;
		}
		else if(goal.toLowerCase().contains("bagfull")){
			this.finished = Inventory.isFull();
			return finished;
		}
		else if(goal.toLowerCase().contains("smith")){
			this.finished = Skills.getRealLevel(Skill.SMITHING) >= Integer.parseInt(goal.split("=")[1]);
			return finished;
		}
		return false;
	}
	
	private GameObject getClosest(List<GameObject> rocks){
		GameObject currRock = null;
		double dist = Double.MAX_VALUE;
		for(GameObject go : rocks){
			if(currRock == null){
				currRock = go;
				dist = go.distance(Players.getLocal());
				continue;
			}
			double tempDist = go.distance(Players.getLocal());
			if(tempDist < dist){
				currRock = go;
				dist = tempDist;
			}
		}
		return currRock;
	}
	
	public Filter<GameObject> getRockFilter(){
		return this.rockFilter;
	}
	
	public GameObject getRock(){
		List<GameObject> acceptableRocks = GameObjects.all(rockFilter);//11161,10943,11361,11360,11365
		return getClosest(acceptableRocks);
	}
	
	public boolean isPowerMine(){
		return powermine;
	}
	public boolean isSmith(){
		return smith;
	}
	public Tile getStartTile(){
		return startTile;
	}
	public String getGoal(){
		return goal;
	}
	public PricedItem getTracker(){
		return oreTracker;
	}
	public String getOreName(){
		return oreName;
	}
	public Timer getTimer(){
		return t;
	}
	public Ores getOres(){
		return this.ores;
	}
	public int[] getIDs(){
		return ids;
	}
	public BankLocation getBank(){
		return bank;
	}
	public boolean getFinished(){
		return this.finished;
	}
	public void setDontMove(boolean dontMove){
		this.dontMove = dontMove;
	}
	public void setPowerMine(boolean powermine){
		this.powermine = powermine;
	}
	public void setStarTile(Tile startTile){
		this.startTile = startTile;
	}
	public void setGoal(String goal){
		this.goal = goal;
	}
	public void setTracker(PricedItem oreTracker){
		this.oreTracker = oreTracker;
	}
	public void setOreName(String oreName){
		this.oreName = oreName;
	}
	public void setIDs(int[] ids){
		this.ids = ids;
	}
	/*
	public void setContext(MethodContext ctx){
		this.ctx = ctx;
	}
	*/
	public void setBank(BankLocation bank){
		this.bank = bank;
	}
	public void setFinished(boolean finished){
		this.finished = finished;
	}
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Ore Name: " + oreName + "\n");
		sb.append("IDs: ");
		for(int i =0; i < ids.length; i++){
			if(ids[i] > 0)
				sb.append(ids[i]);
			if(i != ids.length-1 && ids[i+1] != 0){
				sb.append(",");
			}
		}
		sb.append("\n");
		sb.append("Tile: " + startTile.toString() + "\n");
		sb.append("Bank: " + bank.toString() + "\n");
		sb.append("Goal: " + goal + "\n");
		sb.append("Powermine: " + powermine + "\n");
		sb.append("Don't Move: " + dontMove + "\n");
		sb.append("Ores: " + ores);
		return sb.toString();
	}
}
