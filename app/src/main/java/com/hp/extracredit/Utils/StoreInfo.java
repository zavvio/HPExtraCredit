package com.hp.extracredit.Utils;

import com.hp.extracredit.R;

/**
 * Created by JohnYang on 4/7/17.
 */

public class StoreInfo {
    public static final String STORE_SELECTION = "store_sel";

    public static final String[] REWARDS =
            new String[] { "Nike", "Baskin Robins", "Nestle", "HP", "Amazon.com", "85C"};

    public static final int[] STORE_images =
            new int[] { R.drawable.store_nike, R.drawable.store_2, R.drawable.store_3, R.drawable.store_hp, R.drawable.store_5, R.drawable.store_6};

    public static final int[] nike_images =
            new int[] { R.drawable.nike1, R.drawable.nike_2};
    public static final int[] br_images =
            new int[] { R.drawable.bas_1, R.drawable.bas_1};
    public static final int[] nestle_image =
            new int[] { R.drawable.nestle1, R.drawable.kitkat, R.drawable.nestle3};
    public static final int[] hp_images =
            new int[] { R.drawable.hp_1, R.drawable.hp2};
    public static final int[] amazon_images =
            new int[] { R.drawable.hp_1, R.drawable.hp2};
    public static final int[] c85_images =
            new int[] { R.drawable.hp_1, R.drawable.hp2};

    public static final String[] nike_des =
            new String[] { "Nike shirt" , "Nike shoes"};
    public static final String[] br_des =
            new String[] { "IceCream1" , "IceCream2"};
    public static final String[] nestle_des =
            new String[] { "Espressel" , "KitKat", "Water"};
    public static final String[] hp_des =
            new String[] { "Hp Sprocket" , "Hp Laptop"};
    public static final String[] amazon_des =
            new String[] { "Nike shirt" , "Nike shoes"};
    public static final String[] c85_des =
            new String[] { "Nike shirt" , "Nike shoes"};


    public static int[] getProduct(int storeNum) {
        switch (storeNum) {
            case 0:
                return nike_images;
            case 1:
                return br_images;
            case 2:
                return nestle_image;
            case 3:
                return hp_images;
            case 4:
                return amazon_images;
            case 5:
                return c85_images;
            default:
                return nestle_image;
        }
    }

    public static String[] getProductDes(int storeNum) {
        switch (storeNum) {
            case 0:
                return nike_des;
            case 1:
                return br_des;
            case 2:
                return nestle_des;
            case 3:
                return hp_des;
            case 4:
                return amazon_des;
            case 5:
                return c85_des;
            default:
                return nestle_des;
        }
    }

}
