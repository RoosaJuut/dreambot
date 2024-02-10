package roosajuut.dreambot.powerminer.gui;

import roosajuut.dreambot.filemethods.FileMethods;
import roosajuut.dreambot.scriptmain.powerminer.MineTask;
import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import roosajuut.dreambot.scriptmain.powerminer.Ores;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class minerGui extends JFrame {

	/**
	 *
	 */
	String[] optionsToChoose = {"Apple", "Orange", "Banana", "Pineapple", "None of the listed"};

	private static final long serialVersionUID = 1L;
	private int[] ids;
	private JPanel contentPane;
	private JTextField oreID;
	private JTextField goalField;
	private JTextField oreNameField;
	private JTextField tileField;
	JList<String> list = new JList<String>();
	final JCheckBox chckbxDontMove = new JCheckBox("Don't Move");
	FileMethods fm = new FileMethods("DreambotMiner");


	private DefaultListModel<String> model1 = new DefaultListModel<String>();
	private JTextField txtFilename;

	public minerGui(final ScriptVars var) {
		setTitle("Dreambot Power Miner");
		setIconImage(Toolkit.getDefaultToolkit().getImage(minerGui.class.getResource("/javax/swing/plaf/metal/icons/ocean/computer.gif")));
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 419, 322);
		contentPane = new JPanel();
		contentPane.setBackground(SystemColor.inactiveCaptionText);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblEnterTree = new JLabel("Enter Ore IDs:");
		lblEnterTree.setForeground(Color.WHITE);
		lblEnterTree.setBounds(10, 11, 78, 14);
		contentPane.add(lblEnterTree);

		oreID = new JTextField();
		oreID.setText("0,1,2");
		oreID.setBounds(86, 11, 120, 20);
		contentPane.add(oreID);
		oreID.setColumns(10);

		JLabel lblTrainTilLevel = new JLabel("Goal:");
		lblTrainTilLevel.setForeground(Color.WHITE);
		lblTrainTilLevel.setBounds(10, 36, 35, 14);
		contentPane.add(lblTrainTilLevel);

		goalField = new JTextField();
		goalField.setText("Level=99");
		goalField.setBounds(54, 33, 76, 20);
		contentPane.add(goalField);
		goalField.setColumns(10);

		final JComboBox<BankLocation> comboBox = new JComboBox<BankLocation>();
		comboBox.setModel(new DefaultComboBoxModel<BankLocation>(BankLocation.values()));
		comboBox.setBounds(54, 58, 152, 20);
		contentPane.add(comboBox);

		//final JComboBox<GameObject> comboBox1 = new JComboBox<GameObject>();
		//comboBox1.setModel(new DefaultComboBoxModel<GameObject>((Vector<GameObject>) GameObjects.all(rockFilter))
		//comboBox1.setBounds(173, 260, 57, 23);
		//contentPane.add(comboBox1);

		JComboBox<Ores> oreList = new JComboBox<Ores>();
		oreList.setModel(new DefaultComboBoxModel<Ores>(Ores.values()));
		oreList.setBounds(250, 250, 150, 23);
		contentPane.add(oreList);

		final JCheckBox chckbxPowermine = new JCheckBox("Powermine");
		final JCheckBox chckbxSmith = new JCheckBox("Smith");
		final JCheckBox chckbxCombat = new JCheckBox("Combat");


		JButton btnNewButton = new JButton("Start");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				var.started = true;
				dispose();
			}
		});
		btnNewButton.setBounds(10, 215, 393, 23);
		contentPane.add(btnNewButton);

		JLabel lblBank = new JLabel("Bank:");
		lblBank.setForeground(Color.WHITE);
		lblBank.setBounds(10, 61, 46, 14);
		contentPane.add(lblBank);

		chckbxPowermine.setForeground(Color.WHITE);
		chckbxPowermine.setBackground(Color.BLACK);
		chckbxPowermine.setBounds(54, 80, 97, 23);
		contentPane.add(chckbxPowermine);

		chckbxSmith.setForeground(Color.WHITE);
		chckbxSmith.setBackground(Color.BLACK);
		chckbxSmith.setBounds(54, 100, 97, 23);
		contentPane.add(chckbxSmith);

		chckbxCombat.setForeground(Color.WHITE);
		chckbxCombat.setBackground(Color.BLACK);
		chckbxCombat.setBounds(153, 100, 97, 23);
		contentPane.add(chckbxCombat);

		JLabel lblNewLabel = new JLabel("Ore Name:");
		lblNewLabel.setForeground(Color.WHITE);
		lblNewLabel.setBounds(10, 125, 67, 14);
		contentPane.add(lblNewLabel);

		oreNameField = new JTextField();
		oreNameField.setText("Iron ore");
		oreNameField.setBounds(76, 122, 86, 20);
		contentPane.add(oreNameField);
		oreNameField.setColumns(10);

		JButton btnNewButton_1 = new JButton("Add Task");
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String[] oreids = oreID.getText().split(",");
				int[] ids = new int[oreids.length];
				for(int i = 0; i < ids.length; i++){
					ids[i] = Integer.parseInt(oreids[i]);
				}
				String goal = goalField.getText();
				String oreName = oreNameField.getText();
				BankLocation bank = BankLocation.values()[comboBox.getSelectedIndex()];
				Ores ores = Ores.values()[oreList.getSelectedIndex()];
				boolean powermine = chckbxPowermine.isSelected();
				boolean smith = chckbxSmith.isSelected();
				boolean combat = chckbxCombat.isSelected();
				String tileText = tileField.getText();
				String[] tileVals = tileText.split(",");
				int x = Integer.parseInt(tileVals[0]);
				int y = Integer.parseInt(tileVals[1]);
				int z = Integer.parseInt(tileVals[2]);
				Tile startTile = new Tile(x,y,z);
				boolean dontMove = chckbxDontMove.isSelected();
				MineTask mt = new MineTask(oreName, ids, startTile, goal, powermine, smith, bank, dontMove, ores, combat);
				var.tasks.add(mt);
				model1.addElement(mt.toString());
				list.setModel(model1);
			}
		});
		btnNewButton_1.setBounds(10, 181, 228, 23);
		contentPane.add(btnNewButton_1);

		list.setVisibleRowCount(12);
		list.setToolTipText("");
		list.setModel(model1);
		list.setBackground(Color.LIGHT_GRAY);
		list.setForeground(Color.WHITE);
		list.setBounds(251, 11, 152, 167);
		contentPane.add(list);

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setToolTipText("hi");
		separator.setBackground(Color.WHITE);
		separator.setBounds(248, 0, 3, 213);
		contentPane.add(separator);

		JButton btnNewButton_2 = new JButton("Remove Task");
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String selected = list.getSelectedValue();
				if(selected != null){
					int index = list.getSelectedIndex();
					var.tasks.remove(index);
					model1.removeElement(selected);
					list.setModel(model1);
				}
			}
		});
		btnNewButton_2.setBounds(251, 181, 152, 23);
		contentPane.add(btnNewButton_2);

		JLabel lblTile = new JLabel("Tile:");
		lblTile.setForeground(Color.WHITE);
		lblTile.setBounds(10, 150, 46, 14);
		contentPane.add(lblTile);

		tileField = new JTextField();
		Tile myTile = null;
		String tile = "1234,1234,0";
		if(Client.isLoggedIn()){
			myTile = Players.getLocal().getTile();
		}
		if(myTile != null)
			tile = myTile.getX() + "," + myTile.getY() + "," + myTile.getZ();
		tileField.setText(tile);
		tileField.setBounds(76, 147, 86, 20);
		contentPane.add(tileField);
		tileField.setColumns(10);

		chckbxDontMove.setBackground(Color.BLACK);
		chckbxDontMove.setForeground(Color.WHITE);
		chckbxDontMove.setBounds(153, 80, 85, 23);
		contentPane.add(chckbxDontMove);

		txtFilename = new JTextField();
		txtFilename.setText("ironore");
		txtFilename.setBounds(10, 249, 86, 20);
		contentPane.add(txtFilename);
		txtFilename.setColumns(10);

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveFile(var, txtFilename.getText());
			}
		});
		btnSave.setBounds(106, 248, 57, 23);
		contentPane.add(btnSave);

		JButton btnLoad = new JButton("Load");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadFile(var,txtFilename.getText());
			}
		});
		btnLoad.setBounds(173, 248, 57, 23);
		contentPane.add(btnLoad);
	}

	public void saveFile(final ScriptVars var, String fileName){
		//StringBuilder sb = new StringBuilder();
		List<String> sb = new ArrayList<String>();
		for(MineTask mt : var.tasks){
			//add name
			sb.add(mt.getOreName());
			//sb.add("\n");
			//add id's
			StringBuilder idString = new StringBuilder();
			for(int i = 0; i < mt.getIDs().length; i++){
				idString.append(mt.getIDs()[i]);
				if(i < (mt.getIDs().length - 1))
					idString.append(",");
			}
			sb.add(idString.toString());
			//sb.add("\n");
			//add tile
			sb.add(mt.getStartTile().getX()+"," + mt.getStartTile().getY() + "," + mt.getStartTile().getZ());
			//sb.add("\n");
			//add goal
			sb.add(mt.getGoal());
			//sb.add("\n");
			//add powermine
			sb.add(""+mt.isPowerMine());
			//sb.add("\n");
			//add bank
			sb.add(""+mt.isSmith());
			//sb.add("\n");
			//add bank
			sb.add(mt.getBank().toString());
			//sb.add("\n");
			//add don't move
			sb.add(""+mt.dontMove());
			//sb.add("\
			sb.add(""+mt.getOres());
			//
			sb.add(""+mt.isCombat());
		}
		//sb.add("END FILE");
		fm.writeFile(sb, fileName);
	}
	public void loadFile(final ScriptVars var, String fileName){
		var.tasks.clear();
		model1.clear();
		String[] content = fm.readFileArray(fileName);
		//(ctx, oreName, ids, startTile, goal, powermine, bank, dontMove);
		for(int i = 0; i < content.length; i+=9){
			//get name
			String oreName = content[i];
			//get ids
			String idString = content[i+1];
			String[] idsString = idString.split(",");
			int[] ids = new int[idsString.length];
			for(int ii = 0; ii < ids.length; ii++){
				ids[ii] = Integer.parseInt(idsString[ii]);
			}
			//get tile
			String tileString = content[i+2];
			String[] tileStringSplit = tileString.split(",");
			Tile startTile = new Tile(Integer.parseInt(tileStringSplit[0]),Integer.parseInt(tileStringSplit[1]),Integer.parseInt(tileStringSplit[2]));
			//get goal
			String goal = content[i+3];
			//get powermine
			String powermineString = content[i+4];
			boolean powermine = false;
			if(powermineString.toLowerCase().equals("true")){
				powermine = true;
			}
			String smithString = content[i+5];
			boolean smith = false;
			if(smithString.toLowerCase().equals("true")){
				smith = true;
			}
			//get bank
			String bankString = content[i+6];
			BankLocation bank = null;
			for(BankLocation b : BankLocation.values()){
				if(b.toString().equals(bankString)){
					bank = b;
					break;
				}
			}
			//get dont move
			String dontMoveString = content[i+7];
			boolean dontMove = false;
			if(dontMoveString.toLowerCase().equals("true"))
				dontMove = true;
			//ores
			String oreString = content[i+8];
			Ores ores = null;
			for(Ores b : Ores.values()){
				if(b.toString().equals(oreString)){
					ores = b;
					break;
				}
			}
			String combatString = content[i+9];
			boolean combat = false;
			if(combatString.toLowerCase().equals("true"))
				combat = true;

			MineTask mt = new MineTask(oreName, ids, startTile, goal, powermine, smith, bank, dontMove, ores, combat);
			System.out.println(mt.toString());
			var.tasks.add(mt);
			model1.addElement(mt.toString());
			list.setModel(model1);
		}
	}
}
