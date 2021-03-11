package com.sx4.bot.entities.economy.item;

import org.bson.Document;

public class Item {

	private final long price;
	private final String name;
	private final ItemType type;
	
	public Item(String name, long price, ItemType type) {
		this.price = price;
		this.name = name;
		this.type = type;
	}
	
	public long getPrice() {
		return this.price;
	}
	
	public boolean isBuyable() {
		return this.price != -1;
	}
	
	public String getName() {
		return this.name;
	}
	
	public ItemType getType() {
		return this.type;
	}
	
	public Document toData() {
		return new Document("name", this.name)
			.append("type", this.type.getType());
	}
	
}
