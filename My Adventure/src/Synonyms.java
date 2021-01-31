import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.*;

public class Synonyms {
	HashMap<String, String[]> allSynonyms;
	public Synonyms() {
		allSynonyms = new HashMap<String, String[]>();
	}
	public int size() {
		return allSynonyms.size();
	}
	public void addSynonymsBySplit(String val) {
		//System.out.println("DEBUG: addSynonymsBySplit("+val+"): Entering");
		addSynonymPermutations(val.replace("'", "").split(",\\s*"));
	}
	public void addSynonymPermutations(String[] vals) {
		//System.out.println("DEBUG: addSynonymPermutations(vals.length="+vals.length+"): Entering");
		for (String v: vals) {
			allSynonyms.put(v, vals);
		}
	}
	public String[] getSynonyms(String key) {
		return allSynonyms.get(key);
	}
	public String getMatchingSynonym(String key, HashMap<String, String> hayStack) {
		if (!allSynonyms.containsKey(key))
			return null;
		for (String k: allSynonyms.get(key)) {
			if (hayStack.get(k) != null) {
				return k;
			}
		}
		return null;
	}
	public String getSynonymsAsString(String key) {
		String[] syns = getSynonyms(key);
		if (syns == null || syns.length <= 0) return "";
		return " ( "+String.join(", ",  syns)+" )";
	}
	public void export(FileWriter f) {
		try {
			for (String k: allSynonyms.keySet()) {
				if (allSynonyms.get(k).length > 1 && k.equals(allSynonyms.get(k)[0]) || !Arrays.asList(allSynonyms.get(k)).contains(k)) {
					f.write("ALIAS: ("+String.join(",", allSynonyms.get(k))+")\n");
				}
			}
		} catch (IOException e) {
			  System.out.println("An error occurred writing synonyms.");
			  e.printStackTrace();
			  //return false;
		}
	}
}
