package com.sx4.bot.entities.economy.item;

import org.bson.Document;

import java.util.function.BiFunction;

public enum ItemType {

	// Determines in which order items are loaded from the json file
	MATERIAL(0, "Material", 500, Material.class, Material::new),
	WOOD(1, "Wood", 250, Wood.class, Wood::new),
	ROD(2, "Fishing Rod", 1, Rod.class, Rod::new),
	PICKAXE(3, "Pickaxe", 1, Pickaxe.class, Pickaxe::new),
	AXE(4, "Axe", 1, Axe.class, Axe::new),
	MINER(5, "Miner", 10, Miner.class, Miner::new),
	FACTORY(6, "Factory", 10, Factory.class, Factory::new),
	CRATE(7, "Crate", 50, Crate.class, Crate::new),
	ENVELOPE(8, "Envelope", 50, Envelope.class, Envelope::new),
	BOOSTER(9, "Booster", 50, Booster.class, Booster::new);
	
	private final int type;
	private final long defaultLimit;
	
	private final String name;
	private final String dataName;

	private final Class<? extends Item> itemClass;

	@SuppressWarnings("rawtypes")
	private final BiFunction createFunction;
	
	private <Type extends Item> ItemType(int type, String name, long defaultLimit, Class<Type> itemClass, BiFunction<Document, Type, Type> createFunction) {
		this.type = type;
		this.name = name;
		this.itemClass = itemClass;
		this.dataName = name.toLowerCase().replace(" ", "_");
		this.defaultLimit = defaultLimit;
		this.createFunction = createFunction;
	}
	
	public int getType() {
		return this.type;
	}
	
	public long getDefaultLimit() {
		return this.defaultLimit;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDataName() {
		return this.dataName;
	}

	public Class<? extends Item> getItemClass() {
		return this.itemClass;
	}

	@SuppressWarnings("unchecked")
	public <Type extends Item> Type create(Document data, Type defaultItem) {
		return (Type) this.createFunction.apply(data, defaultItem);
	}
	
	public static ItemType fromType(int type) {
		for (ItemType itemType : ItemType.values()) {
			if (itemType.getType() == type) {
				return itemType;
			}
		}
		
		return null;
	}
	
	public static ItemType fromName(String name) {
		for (ItemType itemType : ItemType.values()) {
			if (itemType.getName().equalsIgnoreCase(name)) {
				return itemType;
			}
		}
		
		return null;
	}
	
	public static ItemType fromDataName(String dataName) {
		for (ItemType itemType : ItemType.values()) {
			if (itemType.getDataName().equals(dataName)) {
				return itemType;
			}
		}
		
		return null;
	}
	
}
