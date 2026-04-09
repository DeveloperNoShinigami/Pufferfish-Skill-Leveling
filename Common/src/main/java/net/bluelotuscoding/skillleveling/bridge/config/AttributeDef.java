package net.bluelotuscoding.skillleveling.bridge.config;

import net.puffish.skillsmod.expression.Expression;

public class AttributeDef {
    public String id;
    public String name;
    public String attribute_id;
    public String icon;
    public String description;
    public String format;
    public String value;
    public String operation;
    public int max_points;
    public String command = null;
    public String point_cost = null;

    public transient Expression<Double> compiledExpression;
    public transient Expression<Double> compiledPointCostExpression;
}
