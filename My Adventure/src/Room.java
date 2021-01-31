import java.io.*;
import java.util.*;

public class Room {
  public String name;
  public String shortDescription;
  public String longDescription;
  public ArrayList<Item> items;
  public HashMap<String, String> verbNames = new HashMap<String, String>();
  //public HashMap<String, Room> nav;
  public boolean iveBeenHere = false;

// this is due to laziness
  public Room(String roomName, String sDesc, String lDesc) {
	  //System.out.println("DEBUG: Room(): Adding "+roomName+"("+sDesc+")("+lDesc+")");
	name = roomName;
	if (sDesc.equals("!!null")) sDesc = lDesc;
	shortDescription = sDesc;
	longDescription = lDesc;
	items = new ArrayList<Item>();
  }
  public void addItem(Item i) {
	  items.add(i);
  }
  public Item removeItem(String name) {
	  for (int i=items.size()-1; i>=0; i--)
		  if (items.get(i).getName().equals(name)) {
			  System.out.println("Found " + name);
			  return(items.remove(i));
		  } else {
			  System.out.println("NO MATCH: " + items.get(i).getName());
		  }
	  System.out.println("DEBUG: removeItem(): Not found");
	  return null;
  }
  public ArrayList<Item> getItems() {
	  return items;
  }
  
  public void addVerbName(String verb, String name) {
    verbNames.put(verb, name);
  }
  public String getDestFromVerb(String verb) {
	  return verbNames.get(verb);
  }
  /*
   * 
   */
  public void export(FileWriter f) {
	  try {
		  f.write("ROOM: NAME:"+name+" SHORTDESC:{"+shortDescription+"} LONGDESC:{"+longDescription.replace("\n", "\\n")+"}\n");
		  for (String v: verbNames.keySet()) {
			  f.write("VERB: (ROOM="+name+") "+v+":"+verbNames.get(v)+"\n");
		  }
		  f.write("END ROOM "+name+"\n");
	  } catch (IOException e) {
		  System.out.println("An error occurred writing rooms.");
		  e.printStackTrace();
		  //return false;
	  }
  }
  /*
  public void resolveNav() {
    for (Map.Entry<String, String> n : navNames.entrySet()) {
      nav.put(n.getKey(), something.findRoom(n.getValue()));
    }
  }
  */
  /*
	public Room(String sDesc, String lDesc, Room n, Room e, Room s, Room w, Room u, Room d) {
		shortDescription = sDesc;
		longDescription = lDesc;
		this.setDirections(n, e, s, w, u, d);
	}
	public void setDirections(Room n, Room e, Room s, Room w, Room u, Room d) {
		northRoom = n;
		eastRoom = e;
		southRoom = s;
		westRoom = w;
		upRoom = u;
		downRoom = d;
	}
  */
  public void printDescription() {
    if (iveBeenHere) {
      System.out.println(shortDescription);
    } else {
      System.out.println(longDescription);
      iveBeenHere = true;
    }
    if (items.size() > 0) {
    	System.out.println("Items:");
    	for (Item i: items) {
    		System.out.println(i.getDesc() + " ("+i.getName()+")");
    	}
    }
  }
}