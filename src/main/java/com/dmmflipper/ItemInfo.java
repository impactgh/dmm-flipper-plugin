package com.dmmflipper;

import lombok.Data;

@Data
public class ItemInfo
{
	private int id;
	private String name;
	private String examine;
	private boolean members;
	private int lowalch;
	private int highalch;
	private int limit;
	private int value;
	private String icon;
}
