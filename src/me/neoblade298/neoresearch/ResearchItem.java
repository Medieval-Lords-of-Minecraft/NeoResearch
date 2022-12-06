package me.neoblade298.neoresearch;

import java.util.HashMap;

public class ResearchItem {
	private String name;
	private HashMap<String, Integer> goals;
	private int attrs;
	private int exp;
	private String id;
	
	public ResearchItem(String name) {
		this.goals = new HashMap<String, Integer>();
		this.attrs = 0;
		this.exp = 0;
		this.name = name;
	}

	public HashMap<String, Integer> getGoals() {
		return goals;
	}

	public void setGoals(HashMap<String, Integer> goals) {
		this.goals = goals;
	}

	public int getAttrs() {
		return attrs;
	}

	public void setAttrs(int attrs) {
		this.attrs = attrs;
	}

	public int getExp() {
		return exp;
	}

	public void setExp(int exp) {
		this.exp = exp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
