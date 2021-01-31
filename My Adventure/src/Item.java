import java.io.*;

public class Item {
	String name;
	String description;
	String invDescription;
	String startRoom;
	
	public Item(String n, String d, String iD, String sR) {
		name = n;
		description = d;
		invDescription = iD;
		startRoom = sR;
	}
	public String getName() {
		return name;
	}
	public String getDesc() {
		return description;
	}
	public String getInvDesc() {
		return invDescription;
	}
	public void export(FileWriter f) {
		try {
			f.write("ITEM: NAME:"+name+" DESCRIPTION:{"+description+"} INVENTORY_DESC:{"+invDescription+"} STARTROOM={"+startRoom+"}\n");
		} catch (IOException e) {
			System.out.println("An error occurred writing items.");
			e.printStackTrace();
			//return false;
		}
	}
}
