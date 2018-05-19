package yaossg.mod.mana_craft;

import yaossg.mod.mana_craft.api.ManaCraftAPIs;

import java.util.ArrayList;
import java.util.List;

public class API extends ManaCraftAPIs {
    public static final List<Recipe> recipes = new ArrayList<>();
    public static final List<Fuel> fuels = new ArrayList<>();
    public static final API INSTANCE = new API();

    @Override
    public List<Recipe> getRecipes() {
        return recipes;
    }

    @Override
    public List<Fuel> getFuel() {
        return fuels;
    }
}
