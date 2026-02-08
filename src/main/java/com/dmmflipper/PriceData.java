package com.dmmflipper;

import lombok.Data;

@Data
public class PriceData
{
	private int high;
	private int low;
	private long highTime;
	private long lowTime;
	private int highVolume;
	private int lowVolume;
}
