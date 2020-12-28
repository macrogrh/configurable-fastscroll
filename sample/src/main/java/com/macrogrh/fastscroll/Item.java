package com.macrogrh.fastscroll;

import java.util.ArrayList;
import java.util.Random;

public class Item {
    private String title;
    private String body;

    public Item(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public static ArrayList<Item> createList(int numContacts, boolean fixedHeight) {
        int count = 0;
        Random random = new Random();
        ArrayList<Item> items = new ArrayList<Item>();

        for (int i = 1; i <= numContacts; i++) {
            String body = "";
            if( !fixedHeight ) {
                int bodyCountRandom = Math.abs(random.nextInt(10));
                for (int j = 1; j <= bodyCountRandom; j++) {
                    body += j + "\n";
                }
            }
            items.add(new Item("Item " + ++count, body));
        }

        return items;
    }
}
