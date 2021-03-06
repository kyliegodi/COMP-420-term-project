package edu.csuci.comp420term.entities;

import org.json.JSONObject;

public class Ability extends JSONEntity {

    public final int id;
    public final String name;
    public final String effect;

    public Ability(int id, String name, String effect) {
        this.id = id;
        this.name = name;
        this.effect = effect;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", this.id);
        jsonObject.put("name", this.name);
        jsonObject.put("effect", this.effect);
        return jsonObject;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = obj == this;
        if (!result) {
            if (obj == null || obj.getClass() != this.getClass()) {
                result = false;
            } else {
                Ability other = (Ability) obj;
                result = this.id == other.id;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
