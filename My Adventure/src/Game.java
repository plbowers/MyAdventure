import java.util.*;
import java.io.*;
import java.util.regex.*;
import java.time.*;

public class Game {
	Synonyms allSyns = new Synonyms();
	ArrayList<Item> inventory;
	ArrayList<Item> allItems = new ArrayList<Item>(); // maintained for export purposes
	Room currentRoom = null;
	String startRoom;
	HashMap<String,Room> allRooms = new HashMap<String,Room>();
	public static void main(String[] args) throws FileNotFoundException {
		Game myGame = new Game("adventure.yaml", "LOC_START");

		if (myGame.currentRoom == null) {
			System.out.println("ERROR: nonexistent starting room");
		} else {
			myGame.play();
		}
	}
  
  public void play() {
	  Room newRoom;
	  Item item;
	  Scanner s = new Scanner(System.in);
	  String ans="";
	  Pattern getPat = Pattern.compile("^(?:GET|TAKE)\\s+(\\S+)");
	  Pattern outputPat = Pattern.compile("^(?:EXPORT|OUT(?:PUT)?)\\s+(\\S+)");
	  //currentRoom = initRooms();
	  while (true) {
		  currentRoom.printDescription();
		  printPrompt();
		  ans = s.nextLine().toUpperCase();
		  System.out.println("OK, we'll try "+ans);
		  /*
		   * GET|TAKE <ITEM>
		   */
		  Matcher m = getPat.matcher(ans);
		  if (m.find()) {
			  System.out.println("Trying to pick up \"" + m.group(1) + "\"");
			  item = currentRoom.removeItem(m.group(1));
			  if (item == null) {
				  System.out.println("Hmmm... I don't see \""+m.group(1)+"\" sitting around. Maybe you could rephrase that?");
			  } else {
				  inventory.add(item);
				  System.out.println("Got it!");
			  }
			  continue;
		  }
		  
		  /*
		   * EXPORT|OUTPUT <filename>
		   * This allows us to output the adventure.yaml contents in an easier-to-read
		   * format
		   */
		  m = outputPat.matcher(ans);
		  if (m.find()) {
			  System.out.println("Exporting adventure game to "+m.group(1));
			  if (export(m.group(1)))
				  System.out.println("SUCCESS!\n");
			  else
				  System.out.println("FAILURE!\n");
			  continue;
		  }
		  
		  // other commands I'm interested in are one-word commands and I only look at 5 chars
		  ans = ans.substring(0,Math.min(5, ans.length()));
		  
		  /*
		   * QUIT
		   */
		  if (ans.equals("QUIT")) {
			  System.out.println("Sorry to see you go. Exiting the game.");
			  break;
		  }
		  
		  /*
		   * HELP
		   */
		  if (ans.equals("HELP")) {
			  for (String v: currentRoom.verbNames.keySet())
				  System.out.println("Verb: "+v+allSyns.getSynonymsAsString(v)+" takes you to "+currentRoom.verbNames.get(v));
			  System.out.println("<End of Help>\n");
			  continue;
		  }
		  
		  /* 
		   * LOOK
		   */
		  if (ans.equals("LOOK") || ans.equals("L")) {
			  currentRoom.iveBeenHere = false;
			  continue;
		  }
		  
		  /*
		   * INVENTORY
		   */
		  if (ans.equals("INVEN") || ans.equals("I")) {
			  System.out.println("INVENTORY:");
			  for (Item i: inventory) {
				  System.out.println("ITEM: "+i.getInvDesc());
			  }
			  System.out.println();
			  continue;
		  }
		  
		  /*
		   * All other one-word commands (usually navigation) to correspond to goto actions initiated
		   * by verbs in the travel chunk of each room in the locations section 
		   */
		  ans = allSyns.getMatchingSynonym(ans, currentRoom.verbNames);
		  if (ans == null)
			  newRoom = null;
		  else
			  newRoom = allRooms.get(currentRoom.getDestFromVerb(ans));
		  if (newRoom == null) {
			  System.out.println("Sorry - can't go that way!");
		  } else {
			  currentRoom = newRoom;
		  }
	  }
  }
  public void printPrompt() {
	  System.out.print("\nWhat do you want to do? ");
  }
  public boolean export(String fn) {
	  try {
		  File file = new File(fn);
		  FileWriter fr = new FileWriter(file);
		  fr.write("ADVENTURE FILE WRITTEN: " + LocalDateTime.now()+"\n");
		  fr.write("Starting Room="+startRoom+"\n\n");
		  
		  allSyns.export(fr);
		  fr.write("\n");
		  for (String r: allRooms.keySet()) {
			  allRooms.get(r).export(fr);
		  }
		  fr.write("\n");
		  for (Item i: allItems) {
			  i.export(fr);
		  }
		  fr.write("\n");
		  		  
		  fr.close();
		  return true;
	  } catch (IOException e) {
		  System.out.println("An error occurred.");
		  e.printStackTrace();
		  return false;
	  }
  }
  
  public Game(String fn, String startRoom) throws FileNotFoundException {
	this.startRoom = startRoom; // keep track of this for export purposes
    String roomName="", longDesc="", shortDesc="";
    
    Room newRoom;
    String itemName="", itemRoom="", itemDesc="", itemInvDesc="";
    ArrayList<String> verbs = new ArrayList<String>();
    ArrayList<String> gotos = new ArrayList<String>();
    File file = new File(fn);
    Scanner s = new Scanner(file);
    String line;
    int state = -100; // -20 starts motions, -10 starts hints, 0 starts locations
    int curDescType = 0;
    Matcher m,m1;
    Pattern motionStart = Pattern.compile("^motions:");
    Pattern motionDef = Pattern.compile("^\\s*words: \\[([^]]*)\\]");
    Pattern hintStart = Pattern.compile("^hints:");
    Pattern locStart = Pattern.compile("^locations:");
    Pattern locName = Pattern.compile("^- (LOC_[^ ]*):\\s*");
    Pattern descPat = Pattern.compile("^ *description:");
    Pattern locEnd = Pattern.compile("^arbitrary_messages:");
    Pattern locLongShort = Pattern.compile("^ *(long|short): '?(.*?)'?\\s*$", Pattern.DOTALL); // non-greedy
    Pattern allPostWhite = Pattern.compile("^\\s*(\\S.*)$");
    Pattern locCondition = Pattern.compile("^\\s*conditions:");
    Pattern locTravel = Pattern.compile("^\\s*travel:\\s*(.*)");
    Pattern locTravelVerbs = Pattern.compile("^\\s*\\{verbs:\\s*\\[([^]]*)\\],.*action:\\s*\\[goto,\\s*([^]]*)\\]\\},?");
    Pattern endTravelVerbs = Pattern.compile("^\\s*\\]\\s*$");
    Pattern itemsStart = Pattern.compile("^objects:");
    Pattern itemNamePat = Pattern.compile("^-\\s*(\\S*):\\s*$");
    Pattern itemLocPat = Pattern.compile("^\\s*locations:\\s*\\[?([A-Z_]*)"); // does NOT handle multiple locations
    Pattern itemStartDesc = Pattern.compile("^\\s*descriptions:");
    Pattern itemDescPat = Pattern.compile("^\\s*- '(.*)'");
    Pattern itemInvDescPat = Pattern.compile("^\\s*inventory: '(.*)'");
    Pattern obitStartPat = Pattern.compile("^\\s*obituaries:");
    //Pattern validVerbs = Pattern.compile("(EAST|WEST|NORTH|SOUTH)");
    Pattern validVerbs = Pattern.compile("([A-Z]*)(?:,\\s*)?");
    Pattern longLinePat = Pattern.compile("((\\s*)\\S.*)\\|-");
    Pattern longLineInitWhiteSpace = Pattern.compile(".");
    String prevLine = "", longLineSep="", longLine="";
    boolean readingLongLine = false;
    int lineNo = 0;
    Item it;
    Room tmpRoom;
    while (s.hasNextLine() || prevLine.length() > 0) {
    	/*
    	 * A bit of complication introduced by the |- notation to get long lines
    	 * Basically if we hit that then we just do a quick iteration until we've collected
    	 * ALL the related lines that make up that long line and then do a full iteration
    	 * once using that long line. However, the only way we know that we're done with that
    	 * long line is to have read a line that goes beyond that, so the next time through
    	 * we have to use prevLine (which we've saved off) instead of reading the next line
    	 * from the file.
    	 */
    	if (prevLine.length() > 0) {
    		//System.out.println("Prevline");
    		line = prevLine;
    		prevLine = "";
    	} else {
    		line = s.nextLine().replace("''", "'");
    		lineNo++;
    	}
    	
    	//if (lineNo > 400) continue; // helpful when debugging - adjust line numbers to specify what you want to read
    	
    	m = longLinePat.matcher(line);
    	if (m.find()) {
    		//System.out.println("DEBUG: longLinePat matched");
    		longLine = m.group(1);
    		longLineSep = ""; // don't put a newline in the first one
    		longLineInitWhiteSpace = Pattern.compile("^\\s{"+(m.group(2).length()+2)+","+(m.group(2).length()+4)+"}(.*)$");
    		readingLongLine = true;
    		continue;
    	}
    	if (readingLongLine) {
    		//System.out.println("DEBUG: reading long line");
    		m = longLineInitWhiteSpace.matcher(line);
    		if (m.find()) {
    			longLine += longLineSep + m.group(1);
    			longLineSep = "\n";
    			continue;
    		} else {
    			prevLine = line; // save off for the next iteration through the loop
    			line = longLine.substring(1); // now go through the loop once using this long line
    			longLine = "";
    			readingLongLine = false;
    		}
    	}
    	//System.out.println("DEBUG--: line="+line);
    	/*
    	 * end of long-line complication. Sorry about that.
    	 */

    	/*
    	 * section started by "motion:"
    	 * Basically verb synonyms (N same as NORTH, that kind of thing)
    	 */
    	if (state < -20 && motionStart.matcher(line).find()) {
    		state = -20;
    		continue;
    	}
    	if (state == -20) {
    		//System.out.println("DEBUG: Looking for motion def in "+line);
    		m = motionDef.matcher(line);
    		if (m.find()) {
    			//System.out.println("FOUND "+m.group(1));
    			allSyns.addSynonymsBySplit(m.group(1).toUpperCase());
    			continue;
    		} else if (hintStart.matcher(line).find()) {
    			System.out.println("DEBUG: Read "+allSyns.size()+" motion aliases (synonyms)");
    			state = 0;
    			continue;
    		}
    	}
    	if (state == 0 && locStart.matcher(line).find()) {
    		state = 1; // in location section
    		continue;
    	}
    	if (state < 10 && locEnd.matcher(line).find()) {
    		System.out.println("DEBUG: Read "+allRooms.size()+" rooms");
    		state = 9; // out of location section
    		continue;
    	}

    	if (state > 0 && state < 10) {
    		m = locName.matcher(line);
    		if (m.find()) {
    			if (roomName.length() > 0) {
    				//System.out.println("CREATE ROOM name="+roomName+", long="+longDesc+", short="+shortDesc+", "+verbs.size()+" verbs and gotos");
    				newRoom = new Room(roomName, shortDesc, longDesc);
    				for (int i=0; i<verbs.size(); i++) {
    					String verb = verbs.get(i);
    					String dest = gotos.get(i);
    					m1 = validVerbs.matcher(verb);
    					boolean firstVerb = true;
    					while (m1.find()) {
    						System.out.println("\tVERB="+m1.group(1)+", DEST="+dest);
    						String verbName = "";
    						if (m1.group(1).length() == 0) {
    							if (firstVerb) {
    								verbName = "AUTO";
    							}
    						} else
    							verbName = m1.group(1);
    						if (verbName.length() > 0) {
    							System.out.println("\tDEBUG: Adding verb="+verbName+", DEST="+dest);
    							newRoom.addVerbName(verbName, dest);
    						}
    						firstVerb = false;
    					}
    				}
    				allRooms.put(roomName, newRoom);
    			}
    			roomName = m.group(1);
    			longDesc = shortDesc = "";
    			verbs = new ArrayList<>();
    			gotos = new ArrayList<>();
    			state = 2; // looking for "description:"
    			//System.out.println("DEBUG: Reading room Name="+roomName);
    			continue;
    		}
    	}
    	if (state == 2) {
    		if(descPat.matcher(line).find()) {
    			state = 3; // looking for long: or short:
    			continue;
    		} else {
    			System.out.println("ERROR: Line "+lineNo+": No description tag");
    		}
    	}

    	if (state == 3) {
    		m = locLongShort.matcher(line);
    		if (m.find()) {
    			if (m.group(1).equals("long")) {
    				curDescType = 1; // long
    				if (m.group(2).equals("|-"))
    					longDesc = "";
    				else
    					longDesc = m.group(2);
    			} else { // short
    				curDescType = 2; // short
    				shortDesc = m.group(2);
    				state = 4; // looking for travel:
    				continue; // short is always single line
    			}
    		} else {
    			// continuation of long or short
    			m = allPostWhite.matcher(line);
    			if (!m.find()) {
    				System.out.println("And they go screaming into the night! (" + lineNo +")");
    				System.out.println("line="+line);
    			} else {
    				if (curDescType == 1) { // long
    					longDesc += (longDesc.length()>0?"\n":"") + m.group(1);
    				} else { // short
    					shortDesc += (shortDesc.length()>0?"\n":"") + m.group(1);
    				}
    			}
    		}
    	}
    	if (state == 4) {
    		if (locCondition.matcher(line).find()) {
    			state = 5; // looking for travel:
    			continue;
    		} else {
    			System.out.println("Oops! "+lineNo+": "+line);
    		}
    	}
    	if (state == 5) {
    		m = locTravel.matcher(line);
    		if (m.find()) {
    			//System.out.println("TRAVEL: " + m.group(1));
    			state = 6;
    			continue;
    		}
    	}
    	if (state == 6) {
    		m = locTravelVerbs.matcher(line);
    		if (m.find()) {
    			verbs.add(m.group(1));
    			gotos.add(m.group(2));
    		} else {
    			m = endTravelVerbs.matcher(line);
    			if (m.find()) {
    				state = 7;
    				continue;
    			}
    		}
    	}

    	if (state < 10 && itemsStart.matcher(line).find()) {
    		state = 10; // looking for items
    		continue;
    	}

    	if (state == 10) {
    		m = itemNamePat.matcher(line);
    		if (m.find()) {
    			// if we have read in an item then create it
    			if (itemName.length() > 0) {
    				addItem(itemName, itemDesc, itemInvDesc, itemRoom);
    			}

    			itemName = m.group(1);
    			itemInvDesc = itemDesc = itemRoom = "";
    			continue;
    		}
    		m = itemLocPat.matcher(line);
    		if (m.find()) {
    			itemRoom = m.group(1);
    			continue;
    		}
    		m = itemInvDescPat.matcher(line);
    		if (m.find()) {
    			itemInvDesc = m.group(1);
    			continue;
    		}
    		if (itemDesc.length() <= 0) { // only first description read in
    			m = itemDescPat.matcher(line);
    			if (m.find()) {
    				itemDesc = m.group(1);
    				continue;
    			}
    		}
    	}

    	if (obitStartPat.matcher(line).find()) {
    		System.out.println("DEBUG: Read "+allItems.size()+" items");
    		state = 20;
    		continue;
    	}

    } // while(hasNextLine)
    s.close();
    // handle the last item (should probably make it a function. Sigh.)
    if (itemName.length() > 0) {
    	addItem(itemName, itemDesc, itemInvDesc, itemRoom);
    }
    currentRoom = allRooms.get(startRoom);
    inventory = new ArrayList<Item>();
  } // Game() constructor
  
  public void addItem(String itemName, String itemDesc, String itemInvDesc, String itemRoom) {
	  Item it;
	  Room tmpRoom;
	  if (itemName.length() > 0) {
		  it = new Item(itemName, itemDesc, itemInvDesc, itemRoom);
		  allItems.add(it); // intentionally leaving some bad itemRoom values for students to discover and debug
		  if (itemRoom != null && itemRoom.length() > 0) {
			  tmpRoom = allRooms.get(itemRoom);
			  if (tmpRoom == null) {
				  System.out.println("ERROR: Unknown location \""+itemRoom+"\" for item "+itemName);
			  } else {
				  tmpRoom.addItem(it);
			  }
		  }
	  }
  }
  
}