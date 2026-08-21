package me.justbecause.fastpaintings.compat.distantdecorations;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public record FastPaintingDistantData(Identifier assetId, Direction direction, int width, int height) {}
