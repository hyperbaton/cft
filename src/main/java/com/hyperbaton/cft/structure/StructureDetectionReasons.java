package com.hyperbaton.cft.structure;

import com.hyperbaton.cft.CftMod;
import net.minecraft.resources.ResourceLocation;

/**
 * CFT's reasons a structure detection can fail (or succeed).
 *
 * The progress of a reason measures how far detection got: when several structure types share
 * the same key block and all of them fail, the player is shown only the failure of the type that
 * got closest to being detected, the one with the highest progress. Each stage of detection has
 * its band, and reasons within a stage are 10 apart, so addon reasons can be placed between them:
 * <ul>
 *     <li>0: preconditions, detection could not even start;</li>
 *     <li>100: base shape, the floor, border or first layer;</li>
 *     <li>200: body, the walls, interior, roof and enclosure;</li>
 *     <li>300: proportions, the body exists but has the wrong ones;</li>
 *     <li>400: final checks, the structure is essentially complete;</li>
 *     <li>1000: success.</li>
 * </ul>
 */
public final class StructureDetectionReasons {

    // --- Preconditions: detection could not even start ---
    public static final StructureDetectionReason NOT_A_KEY_BLOCK = reason("not_a_key_block", 0);
    public static final StructureDetectionReason ALREADY_REGISTERED = reason("already_registered", 10);
    public static final StructureDetectionReason OVERLAPPING_STRUCTURE = reason("overlapping_structure", 20);

    // --- Base shape: floor, border or first layer not found or invalid ---
    public static final StructureDetectionReason NO_FLOOR = reason("no_floor", 100);
    public static final StructureDetectionReason FLOOR_TOO_BIG = reason("floor_too_big", 110);
    public static final StructureDetectionReason INVALID_BORDER = reason("invalid_border", 120);
    public static final StructureDetectionReason BORDER_NOT_CLOSED = reason("border_not_closed", 130);
    public static final StructureDetectionReason INVALID_MONUMENT_LAYER = reason("invalid_monument_layer", 140);
    public static final StructureDetectionReason INVALID_FLOOR = reason("invalid_floor", 150);
    public static final StructureDetectionReason INVALID_GROUND_PERIMETER = reason("invalid_ground_perimeter", 160);
    public static final StructureDetectionReason SURFACE_TOO_BIG = reason("surface_too_big", 170);
    public static final StructureDetectionReason INVALID_SURFACE = reason("invalid_surface", 180);

    // --- Body: walls, interior, roof and enclosure ---
    public static final StructureDetectionReason INVALID_WALLS = reason("invalid_walls", 200);
    public static final StructureDetectionReason INVALID_INTERIOR = reason("invalid_interior", 210);
    public static final StructureDetectionReason INVALID_ROOF = reason("invalid_roof", 220);
    public static final StructureDetectionReason CEILING_NOT_FLAT = reason("ceiling_not_flat", 230);
    public static final StructureDetectionReason NO_CLOSURE = reason("no_closure", 240);
    public static final StructureDetectionReason NO_SKY_ACCESS = reason("no_sky_access", 250);

    // --- Proportions: the body exists but has the wrong ones ---
    public static final StructureDetectionReason MONUMENT_TOO_SHORT = reason("monument_too_short", 300);
    public static final StructureDetectionReason MONUMENT_TOO_TALL = reason("monument_too_tall", 310);
    public static final StructureDetectionReason NOT_ENOUGH_STOREYS = reason("not_enough_storeys", 320);
    public static final StructureDetectionReason LAYERS_NOT_IDENTICAL = reason("layers_not_identical", 330);

    // --- Final checks: the structure is essentially complete ---
    public static final StructureDetectionReason MISSING_REQUIRED_STRUCTURES = reason("missing_required_structures", 400);
    public static final StructureDetectionReason NO_CONTAINER = reason("no_container", 410);
    public static final StructureDetectionReason TOO_DARK = reason("too_dark", 420);
    public static final StructureDetectionReason NOT_ENOUGH_ANIMALS = reason("not_enough_animals", 430);
    public static final StructureDetectionReason STRUCTURE_TOO_LARGE = reason("structure_too_large", 440);

    // --- Success ---
    public static final StructureDetectionReason STRUCTURE_DETECTED = reason("structure_detected", 1000);
    /** A registered structure, checked again with the staff, still passes. */
    public static final StructureDetectionReason STRUCTURE_CONFIRMED = reason("structure_confirmed", 1010);

    private StructureDetectionReasons() {
    }

    private static StructureDetectionReason reason(String name, int progress) {
        return new StructureDetectionReason(ResourceLocation.fromNamespaceAndPath(CftMod.MOD_ID, name), progress);
    }
}
