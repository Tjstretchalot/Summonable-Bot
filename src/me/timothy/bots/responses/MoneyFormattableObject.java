package me.timothy.bots.responses;

import java.text.NumberFormat;

import me.timothy.bots.Database;
import me.timothy.bots.FileConfiguration;

public class MoneyFormattableObject implements FormattableObject {
	private static final NumberFormat numFormatter = NumberFormat.getCurrencyInstance();
	private int amount;
	
	public MoneyFormattableObject(int am) {
		amount = am;
	}
	
	public int getAmount() {
		return amount;
	}
	
	public void setAmount(int amount) {
		this.amount = amount;
	}
	
	@Override
	public String toFormattedString(ResponseInfo info, String myName, FileConfiguration config, Database db) {
		return numFormatter.format(amount/100.);
	}
}
