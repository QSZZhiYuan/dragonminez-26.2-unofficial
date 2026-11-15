package com.dragonminez.common.hair;

import com.dragonminez.client.util.ColorUtils;
import net.minecraft.nbt.CompoundTag;

public class HairStrand {
    public static final int MAX_CUBE_COUNT = 8;
    public static final int MAX_LENGTH = 50;
    private int length = 0;

	private float lengthScale = 1.0f;

    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float rotationZ = 0.0f;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    private float cubeWidth = 2.0f;
    private float cubeHeight = 2.0f;
    private float cubeDepth = 2.0f;

    private String color = null;
    private transient float[] rgbColor;

    private float curveX = 0.0f;
    private float curveY = 0.0f;
    private float curveZ = 0.0f;

    private int id = 0;
    
    public HairStrand() {}
    
    public HairStrand(int id) {
        this.id = id;
    }

	public float getLengthScale() { return lengthScale; }
	public void setLengthScale(float scale) { this.lengthScale = scale; }

    public int getLength() { return length; }
    
    public void setLength(int length) {
        this.length = Math.max(0, Math.min(MAX_LENGTH, length));
    }
    
    public void addCube() {
        if (length < MAX_LENGTH) {
            length++;
        }
    }
    
    public void removeCube() {
        if (length > 0) {
            length--;
        }
    }
    
    public boolean isVisible() {
        return length > 0;
    }

    public int getCubeCount() {
        return length;
    }

    public float getStretchFactor() {
		return lengthScale;
    }

    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }
    
    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    
    public void setScale(float x, float y, float z) {
        this.scaleX = Math.max(0.1f, x);
        this.scaleY = Math.max(0.1f, y);
        this.scaleZ = Math.max(0.1f, z);
    }

    public float getCubeWidth() { return cubeWidth; }
    public float getCubeHeight() { return cubeHeight; }
    public float getCubeDepth() { return cubeDepth; }

    public float getCurveX() { return curveX; }
    public float getCurveY() { return curveY; }
    public float getCurveZ() { return curveZ; }
    
    public void setCurve(float x, float y, float z) {
        this.curveX = x;
        this.curveY = y;
        this.curveZ = z;
    }

    public String getColor() { return color; }
    public boolean hasCustomColor() { return color != null && !color.isEmpty(); }

    public float[] getRgbColor() {
        if (rgbColor == null) {
            rgbColor = ColorUtils.hexToRgb(hasCustomColor() ? color : "#FFFFFF");
        }
        return rgbColor;
    }

    public void setColor(String color) {
        this.color = color;
        this.rgbColor = hasCustomColor() ? ColorUtils.hexToRgb(color) : null;
    }

    public int getId() { return id; }
	protected void setId(int id) { this.id = id; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (id != 0) tag.putInt("i", id);
        if (length != 0) tag.putInt("l", length);
		if (lengthScale != 1.0f) tag.putFloat("ls", lengthScale);

        if (rotationX != 0.0f) tag.putFloat("rx", rotationX);
        if (rotationY != 0.0f) tag.putFloat("ry", rotationY);
        if (rotationZ != 0.0f) tag.putFloat("rz", rotationZ);

        if (scaleX != 1.0f) tag.putFloat("sx", scaleX);
        if (scaleY != 1.0f) tag.putFloat("sy", scaleY);
        if (scaleZ != 1.0f) tag.putFloat("sz", scaleZ);

        if (cubeWidth != 2.0f) tag.putFloat("cw", cubeWidth);
        if (cubeHeight != 2.0f) tag.putFloat("ch", cubeHeight);
        if (cubeDepth != 2.0f) tag.putFloat("cd", cubeDepth);

        if (curveX != 0.0f) tag.putFloat("cx", curveX);
        if (curveY != 0.0f) tag.putFloat("cy", curveY);
        if (curveZ != 0.0f) tag.putFloat("cz", curveZ);

        if (color != null) tag.putString("c", color);

        return tag;
    }
    
    public void load(CompoundTag tag) {
        this.id = tag.contains("i") ? tag.getIntOr("i", 0) : tag.getIntOr("Id", 0);
        this.length = tag.contains("l") ? tag.getIntOr("l", 0) : tag.getIntOr("Length", 0);
		this.lengthScale = tag.contains("ls") ? tag.getFloatOr("ls", 0.0F) : (tag.contains("LengthScale") ? tag.getFloatOr("LengthScale", 0.0F) : 1.0f);

        this.rotationX = tag.contains("rx") ? tag.getFloatOr("rx", 0.0F) : tag.getFloatOr("RotX", 0.0F);
        this.rotationY = tag.contains("ry") ? tag.getFloatOr("ry", 0.0F) : tag.getFloatOr("RotY", 0.0F);
        this.rotationZ = tag.contains("rz") ? tag.getFloatOr("rz", 0.0F) : tag.getFloatOr("RotZ", 0.0F);

        this.scaleX = tag.contains("sx") ? tag.getFloatOr("sx", 0.0F) :
                     (tag.contains("ScaleX") ? tag.getFloatOr("ScaleX", 0.0F) : 1.0f);
        this.scaleY = tag.contains("sy") ? tag.getFloatOr("sy", 0.0F) :
                     (tag.contains("ScaleY") ? tag.getFloatOr("ScaleY", 0.0F) : 1.0f);
        this.scaleZ = tag.contains("sz") ? tag.getFloatOr("sz", 0.0F) :
                     (tag.contains("ScaleZ") ? tag.getFloatOr("ScaleZ", 0.0F) : 1.0f);

        this.cubeWidth = tag.contains("cw") ? tag.getFloatOr("cw", 0.0F) :
                        (tag.contains("CubeW") ? tag.getFloatOr("CubeW", 0.0F) : 2.0f);
        this.cubeHeight = tag.contains("ch") ? tag.getFloatOr("ch", 0.0F) :
                         (tag.contains("CubeH") ? tag.getFloatOr("CubeH", 0.0F) : 2.0f);
        this.cubeDepth = tag.contains("cd") ? tag.getFloatOr("cd", 0.0F) :
                        (tag.contains("CubeD") ? tag.getFloatOr("CubeD", 0.0F) : 2.0f);

        this.curveX = tag.contains("cx") ? tag.getFloatOr("cx", 0.0F) : tag.getFloatOr("CurveX", 0.0F);
        this.curveY = tag.contains("cy") ? tag.getFloatOr("cy", 0.0F) : tag.getFloatOr("CurveY", 0.0F);
        this.curveZ = tag.contains("cz") ? tag.getFloatOr("cz", 0.0F) : tag.getFloatOr("CurveZ", 0.0F);

		setColor(tag.contains("c") ? tag.getStringOr("c", "") :
                    (tag.contains("Color") ? tag.getStringOr("Color", "") : null));
    }
    
    public HairStrand copy() {
        HairStrand copy = new HairStrand(this.id);
        copy.length = this.length;
		copy.lengthScale = this.lengthScale;
        copy.rotationX = this.rotationX;
        copy.rotationY = this.rotationY;
        copy.rotationZ = this.rotationZ;
        copy.scaleX = this.scaleX;
        copy.scaleY = this.scaleY;
        copy.scaleZ = this.scaleZ;
        copy.cubeWidth = this.cubeWidth;
        copy.cubeHeight = this.cubeHeight;
        copy.cubeDepth = this.cubeDepth;
        copy.curveX = this.curveX;
        copy.curveY = this.curveY;
        copy.curveZ = this.curveZ;
        copy.color = this.color;
        copy.rgbColor = this.rgbColor != null ? this.rgbColor.clone() : null;
        return copy;
    }
}

