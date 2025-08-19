package ru.ycoord.persistance;


import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnderChestModel {
    private final String owner;



    public static class Slot{
        private ItemStack storedItem = null;

        public Slot(ItemStack storedItem){
            this.storedItem = storedItem;
        }

        public void setItem(ItemStack storedItem){
            this.storedItem = storedItem;
        }

        public ItemStack getItem(){
            return storedItem;
        }
    }

    public static class Page{
        private Map<Integer, Slot>  slots = new ConcurrentHashMap<>();

        public Slot getSlot(int slot){
            if(!slots.containsKey(slot))
                slots.put(slot, new Slot(null));
            return slots.get(slot);
        }

        public Map<Integer, Slot> getSlots() {
            return slots;
        }

        public void setSlot(int slot, ItemStack itemStack) {
            this.slots.put(slot, new Slot(itemStack));
        }

        public void remove(int slot) {
            this.slots.remove(slot);
        }
    }

    public static class Pages{

        private Map<Integer, Page> pages;

        public Pages(Map<Integer, Page> pages){
            this.pages = pages;
        }

        public int getPagesCount(){
            return pages.size();
        }

        public Page getPage(int pageIndex){
            if(!pages.containsKey(pageIndex))
                pages.put(pageIndex, new Page());
            return pages.get(pageIndex);
        }

        public Map<Integer, Page> getPages(){
            return pages;
        }
    }

    private final Pages pages;
    public EnderChestModel(String owner) {
        this.owner = owner;
        pages = new Pages(new HashMap<>());
    }

    public String getOwner() {
        return owner;
    }

    public Pages getPages() {
        return pages;
    }
}
