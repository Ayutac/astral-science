package com.miir.astralscience.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.miir.astralscience.AstralScience;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.registry.Registry;

public class HeatingRecipeSerializer implements RecipeSerializer<HeatingRecipe> {
    public static HeatingRecipeSerializer INSTANCE = new HeatingRecipeSerializer();
    public static final Identifier ID = AstralScience.id("heating");

    @Override
    public HeatingRecipe read(Identifier id, JsonObject json) {
        if (JsonHelper.getObject(json, "ingredient") == null || JsonHelper.getString(json, "result") == null) {
            throw new JsonSyntaxException("A required attribute is missing!");
        }

        JsonElement inputIngredient = JsonHelper.hasArray(json, "ingredient") ? JsonHelper.getArray(json, "ingredient") : JsonHelper.getObject(json, "ingredient");
        Ingredient input = Ingredient.fromJson(inputIngredient);
        String result = JsonHelper.getString(json, "result");
        int outputAmount = JsonHelper.getInt(json, "outputcount", 1);
        Item outputItem = Registries.ITEM.getOrEmpty(new Identifier(result))
                .orElseThrow(() -> new JsonSyntaxException("No such item " + result));
        ItemStack output = new ItemStack(outputItem, outputAmount);
        float experience = JsonHelper.getFloat(json, "experience", 0.0F);
        int cookingTime = JsonHelper.getInt(json, "cookingtime", 300);
        String group = JsonHelper.getString(json,"group", "");

        return new HeatingRecipe(id, group, input, output, experience, cookingTime);
    }


    @Override
    public HeatingRecipe read(Identifier id, PacketByteBuf buf) {
        String group = buf.readString();
        Ingredient input = Ingredient.fromPacket(buf);
        ItemStack output = buf.readItemStack();
        float experience = buf.readFloat();
        int cookingTime = buf.readInt();
        return new HeatingRecipe(id, group, input, output, experience, cookingTime);
    }

    @Override
    public void write(PacketByteBuf buf, HeatingRecipe recipe) {
        buf.writeString(recipe.getGroup());
        recipe.getInput().write(buf);
        buf.writeItemStack(recipe.getOutput());
        buf.writeFloat(recipe.getExperience());
        buf.writeInt(recipe.getCookTime());
    }
}
