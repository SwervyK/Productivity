package com.productivity.Custom;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.productivity.CheckBoxes;
import com.productivity.Productivity;
import com.productivity.Panels.HomePanel;
import com.productivity.Util.JTextFieldLimit;

import net.miginfocom.swing.MigLayout;

public class AddCustomCheckList extends JPanel {
    
    private static final String kCustomPath = Productivity.getInstance().getCurrentCustomPath();
    private static final File kCustomNames = Productivity.getSave("Custom/customNames.TXT");
    private static final int kCharLimit = 10;
    private static final int kMaxCustomCheckLists = 8;
    
    private static ArrayList<String> mNames = new ArrayList<String>();
    private static HashMap<String, CheckBoxes> mCheckBoxes = new HashMap<String, CheckBoxes>();
    private static int mCurrentNumCheckLists = 0;
    private static boolean mWantHome = true;
    private static int mRandomIndex = -1;

    private static JPanel mCustomPanel = new JPanel(new MigLayout("flowy, gap 5px 5px, ins 5" + (Productivity.kMigDebug?", debug":"")));
    
    public AddCustomCheckList() {
        JTextField name = new JTextField();
        name.setDocument(new JTextFieldLimit(kCharLimit));
        name.addActionListener(e -> {
            if (!testValidFileName(name.getText())) {
                JOptionPane.showMessageDialog(this, "Please enter valid name");
                return;
            }
            if (mCurrentNumCheckLists > kMaxCustomCheckLists) {
                JOptionPane.showMessageDialog(this, "Maximum custom checklists reached");
                return;
            }
            if (!mNames.contains(name.getText()) && !name.getText().equals("")) {
                addCheckList(name.getText(), mWantHome);
                mCurrentNumCheckLists++;
                if (getNumberOfChecklists() == 1) {
                    Productivity.getInstance().customCheckListVisibility(true);
                    Productivity.getInstance().repaintFrame();
                }
                name.setText("");
                HomePanel.getInstance().reset();
            }
            else {
                JOptionPane.showMessageDialog(this, "Please enter valid name");
            }
        });
        
        JLabel nameLbl = new JLabel("Name Of Custom Checklist:");
        super.setLayout(new MigLayout((Productivity.kMigDebug?"debug":"")));
        super.add(nameLbl, "wrap");
        super.add(name, "split 2, growx, wrap");
        super.add(mCustomPanel, "grow, push, span");

        SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				loadCheckLists();
			}
		}
		);
    }
    
    private boolean testValidFileName(String text) {
        return text.matches("^[a-zA-Z0-9._ ]+$");
    }

    private static void purgeDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
            purgeDirectory(file);
            file.delete();
        }
    }
    
    public static int getNumberOfChecklists() {
        return readData(kCustomNames).length/2;
    }
    
    public static void loadCheckLists() {
        try {
            String[] data = readData(kCustomNames);
            for (int i = 0; i < data.length; i += 2) {
                mNames.add(data[i]);
                addCheckList(data[i], Boolean.parseBoolean(data[i + 1]));
                mCurrentNumCheckLists++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            File dir = new File(Productivity.getInstance().getCurrentCustomPath());
            purgeDirectory(dir);
            try {
                kCustomNames.createNewFile();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
    
    public static JCheckBox[] getRandomCheckBoxes() {
        if (mNames.size() <= 0 || mCheckBoxes.size() <= 0) {
            mRandomIndex = -1;
            return null;
        }
        int index = (int)(Math.random() * mCheckBoxes.size());
        try {
            if (!mCheckBoxes.get(mNames.get(index)).getHome() || mCheckBoxes.get(mNames.get(index)).getBoxes().length <= 0) {
                if (index < mCheckBoxes.size()) {
                    for (int i = index; i < mCheckBoxes.size(); i++) {
                        if (mCheckBoxes.get(mNames.get(i)).getHome() && mCheckBoxes.get(mNames.get(index)).getBoxes().length > 0) {
                            index = i;
                            break;
                        }
                    }
                }
                if (!mCheckBoxes.get(mNames.get(index)).getHome() || mCheckBoxes.get(mNames.get(index)).getBoxes().length <= 0) {
                    for (int i = index; i >= 0; i--) {
                        if (mCheckBoxes.get(mNames.get(i)).getHome() && mCheckBoxes.get(mNames.get(index)).getBoxes().length > 0) {
                            index = i;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error getting random check box");
        }
        CheckBoxes checkBox = mCheckBoxes.get(mNames.get(index));
        if (checkBox != null && mCheckBoxes.get(mNames.get(index)).getHome() && mCheckBoxes.get(mNames.get(index)).getBoxes().length > 0) {
            mRandomIndex = index;
            return checkBox.getBoxes();
        }
        mRandomIndex = -1;
        return null;
    }

    public static String getrandomName() {
        return (mRandomIndex != -1) ? mNames.get(mRandomIndex) : "";
    }
    
    public static void setCheckList(boolean state, int index, String name) {
        mCheckBoxes.get(name).setSelected(state, index);
    }
    
    private static void addCheckList(String n, boolean home) {
        File name = new File(kCustomPath + n + "Name.TXT");
        File color = new File(kCustomPath + n + "Color.TXT");
        File check = new File(kCustomPath + n + "Check.TXT");
        if (!name.exists() && !color.exists() && !check.exists()) {        
            try {
                name.createNewFile();
                color.createNewFile();
                check.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!mNames.contains(n)) {
            mNames.add(n);
        }
        JButton button = new JButton(n);
        button.addActionListener(e -> {
            mCustomPanel.remove(button);
            Productivity.getInstance().repaintFrame();
            deleteChecklist(n);
            saveChecklists();
            HomePanel.getInstance().reset();
        });
        button.setFocusPainted(false);
        int rows = (int)(mCustomPanel.getHeight() / (button.getPreferredSize().getHeight()+5));
        if (rows <= 0) rows = 1;
        mCustomPanel.add(button, (((mCustomPanel.getComponentCount()+1) % rows == 0)?"wrap":""));
        CheckBoxes checkBox = new CheckBoxes(name, check, color, false, home);
        mCheckBoxes.put(n, checkBox);
        saveChecklists();
        CustomCheckList.getInstance().addCheckList(checkBox, n);
        HomePanel.getInstance().reset();
    }
    
    private static void deleteChecklist(String n) {
        File name = new File(kCustomPath + n + "Name.TXT");
        File color = new File(kCustomPath + n + "Color.TXT");
        File check = new File(kCustomPath + n + "Check.TXT");
        name.delete();
        color.delete();
        check.delete();
        mNames.remove(n);
        mCheckBoxes.remove(n);
        CustomCheckList.getInstance().removeChecklist(n);
        if (mNames.size() <= 0) {
            Productivity.getInstance().customCheckListVisibility(false);
        }
        mCurrentNumCheckLists--;
    }
    
    private static void saveChecklists() {
        String[] data = new String[mNames.size() * 2];
        for (int i = 0; i < data.length; i += 2) {
            data[i] = mNames.get(i/2);
            data[i + 1] = Boolean.toString(mCheckBoxes.get(mNames.get(i/2)).getHome());
        }
        writeData(data, kCustomNames);
    }
    
    private static String[] readData(File file) {
        String[] result = new String[0];
        try {
            result = new String[(int)Files.lines(file.toPath()).count()];
            Scanner scanner = new Scanner(file);
            int index = 0;
            while (scanner.hasNextLine()) {
                result[index] = scanner.nextLine();
                index++;
            }
            scanner.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    private static void writeData(String data, File file) {
        try  {
            FileWriter writer = new FileWriter(file);
            writer.write(data);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void writeData(String[] dataArr, File file) {
        String data = "";
        for (int i = 0; i < dataArr.length; i++) {
            data += (dataArr[i] + "\n");
        }
        writeData(data, file);
    }
}
