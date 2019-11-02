/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

/**
 * An axis aligned bounding box. That is, an un-rotated cuboid.
 * It is represented by its minimum and maximum corners.
 *
 * <p>The box will never be degenerate: the corners are always not equal and
 * respect the minimum and maximum properties.</p>
 *
 * <p>This class is immutable, all objects returned are either new instances or
 * itself.</p>
 */
public class AABB {

    private final Vector3d min;
    private final Vector3d max;
    private Vector3d size = null;
    private Vector3d center = null;

    /**
     * Constructs a new bounding box from two opposite corners.
     * Fails the resulting box would be degenerate (a dimension is 0).
     *
     * @param firstCorner The first corner
     * @param secondCorner The second corner
     */
    public AABB(Vector3i firstCorner, Vector3i secondCorner) {
        this(checkNotNull(firstCorner, "firstCorner").toDouble(), checkNotNull(secondCorner, "secondCorner").toDouble());
    }

    /**
     * Constructs a new bounding box from two opposite corners.
     * Fails the resulting box would be degenerate (a dimension is 0).
     *
     * @param x1 The first corner x coordinate
     * @param y1 The first corner y coordinate
     * @param z1 The first corner z coordinate
     * @param x2 The second corner x coordinate
     * @param y2 The second corner y coordinate
     * @param z2 The second corner z coordinate
     */
    public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this(new Vector3d(x1, y1, z1), new Vector3d(x2, y2, z2));
    }

    /**
     * Constructs a new bounding box from two opposite corners.
     * Fails the resulting box would be degenerate (a dimension is 0).
     *
     * @param firstCorner The first corner
     * @param secondCorner The second corner
     */
    public AABB(Vector3d firstCorner, Vector3d secondCorner) {
        checkNotNull(firstCorner, "firstCorner");
        checkNotNull(secondCorner, "secondCorner");
        this.min = firstCorner.min(secondCorner);
        this.max = firstCorner.max(secondCorner);
        checkArgument(this.min.getX() != this.max.getX(), "The box is degenerate on x");
        checkArgument(this.min.getY() != this.max.getY(), "The box is degenerate on y");
        checkArgument(this.min.getZ() != this.max.getZ(), "The box is degenerate on z");
    }

    /**
     * The minimum corner of the box.
     *
     * @return The minimum corner
     */
    public Vector3d getMin() {
        return this.min;
    }

    /**
     * The maximum corner of the box.
     *
     * @return The maximum corner
     */
    public Vector3d getMax() {
        return this.max;
    }

    /**
     * Returns the center of the box, halfway between each corner.
     *
     * @return The center
     */
    public Vector3d getCenter() {
        if (this.center == null) {
            this.center = this.min.add(getSize().div(2));
        }
        return this.center;
    }

    /**
     * Gets the size of the box.
     *
     * @return The size
     */
    public Vector3d getSize() {
        if (this.size == null) {
            this.size = this.max.sub(this.min);
        }
        return this.size;
    }

    /**
     * Checks if the bounding box contains a point.
     *
     * @param point The point to check
     * @return Whether or not the box contains the point
     */
    public boolean contains(Vector3i point) {
        checkNotNull(point, "point");
        return contains(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Checks if the bounding box contains a point.
     *
     * @param point The point to check
     * @return Whether or not the box contains the point
     */
    public boolean contains(Vector3d point) {
        checkNotNull(point, "point");
        return contains(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Checks if the bounding box contains a point.
     *
     * @param x The x coordinate of the point
     * @param y The y coordinate of the point
     * @param z The z coordinate of the point
     * @return Whether or not the box contains the point
     */
    public boolean contains(double x, double y, double z) {
        return this.min.getX() <= x && this.max.getX() >= x
               && this.min.getY() <= y && this.max.getY() >= y
               && this.min.getZ() <= z && this.max.getZ() >= z;
    }

    /**
     * Checks if the bounding box intersects another.
     *
     * @param other The other bounding box to check
     * @return Whether this bounding box intersects with the other
     */
    public boolean intersects(AABB other) {
        checkNotNull(other, "other");
        return this.max.getX() >= other.getMin().getX() && other.getMax().getX() >= this.min.getX()
               && this.max.getY() >= other.getMin().getY() && other.getMax().getY() >= this.min.getY()
               && this.max.getZ() >= other.getMin().getZ() && other.getMax().getZ() >= this.min.getZ();
    }

    /**
     * Tests for intersection between the box and a ray defined by a starting
     * point and a direction.
     *
     * @param start The starting point of the ray
     * @param direction The direction of the ray
     * @return An intersection point, if any
     */
    public Optional<IntersectionPoint> intersects(Vector3d start, Vector3d direction) {
        checkNotNull(start, "start");
        checkNotNull(direction, "direction");
        // Adapted from: https://github.com/flow/react/blob/develop/src/main/java/com/flowpowered/react/collision/RayCaster.java#L156
        // The box is interpreted as 6 infinite perpendicular places, one for each face (being expanded infinitely)
        // "t" variables are multipliers: start + direction * t gives the intersection point
        // Find the intersections on the -x and +x planes, oriented by direction
        final double txMin;
        final double txMax;
        final Vector3d xNormal;
        if (Math.copySign(1, direction.getX()) > 0) {
            txMin = (this.min.getX() - start.getX()) / direction.getX();
            txMax = (this.max.getX() - start.getX()) / direction.getX();
            xNormal = Vector3d.UNIT_X;
        } else {
            txMin = (this.max.getX() - start.getX()) / direction.getX();
            txMax = (this.min.getX() - start.getX()) / direction.getX();
            xNormal = Vector3d.UNIT_X.negate();
        }
        // Find the intersections on the -y and +y planes, oriented by direction
        final double tyMin;
        final double tyMax;
        final Vector3d yNormal;
        if (Math.copySign(1, direction.getY()) > 0) {
            tyMin = (this.min.getY() - start.getY()) / direction.getY();
            tyMax = (this.max.getY() - start.getY()) / direction.getY();
            yNormal = Vector3d.UNIT_Y;
        } else {
            tyMin = (this.max.getY() - start.getY()) / direction.getY();
            tyMax = (this.min.getY() - start.getY()) / direction.getY();
            yNormal = Vector3d.UNIT_Y.negate();
        }
        // The ray should intersect the -x plane before the +y plane and intersect
        // the -y plane before the +x plane, else it is outside the box
        if (txMin > tyMax || txMax < tyMin) {
            return Optional.empty();
        }
        // Keep track of the intersection normal which also helps with floating point errors
        Vector3d normalMax;
        Vector3d normalMin;
        // The ray intersects only the furthest min plane on the box and only the closest
        // max plane on the box
        double tMin;
        if (tyMin == txMin) {
            tMin = tyMin;
            normalMin = xNormal.negate().sub(yNormal);
        } else if (tyMin > txMin) {
            tMin = tyMin;
            normalMin = yNormal.negate();
        } else {
            tMin = txMin;
            normalMin = xNormal.negate();
        }
        double tMax;
        if (tyMax == txMax) {
            tMax = tyMax;
            normalMax = xNormal.add(yNormal);
        } else if (tyMax < txMax) {
            tMax = tyMax;
            normalMax = yNormal;
        } else {
            tMax = txMax;
            normalMax = xNormal;
        }
        // Find the intersections on the -z and +z planes, oriented by direction
        final double tzMin;
        final double tzMax;
        final Vector3d zNormal;
        if (Math.copySign(1, direction.getZ()) > 0) {
            tzMin = (this.min.getZ() - start.getZ()) / direction.getZ();
            tzMax = (this.max.getZ() - start.getZ()) / direction.getZ();
            zNormal = Vector3d.UNIT_Z;
        } else {
            tzMin = (this.max.getZ() - start.getZ()) / direction.getZ();
            tzMax = (this.min.getZ() - start.getZ()) / direction.getZ();
            zNormal = Vector3d.UNIT_Z.negate();
        }
        // The ray intersects only the furthest min plane on the box and only the closest
        // max plane on the box
        if (tMin > tzMax || tMax < tzMin) {
            return Optional.empty();
        }
        // The ray should intersect the closest plane outside first and the furthest
        // plane outside last
        if (tzMin == tMin) {
            normalMin = normalMin.sub(zNormal);
        } else if (tzMin > tMin) {
            tMin = tzMin;
            normalMin = zNormal.negate();
        }
        if (tzMax == tMax) {
            normalMax = normalMax.add(zNormal);
        } else if (tzMax < tMax) {
            tMax = tzMax;
            normalMax = zNormal;
        }
        // Both intersection points are behind the start, there are no intersections
        if (tMax < 0) {
            return Optional.empty();
        }
        // Find the final intersection multiplier and normal
        final double t;
        Vector3d normal;
        if (tMin < 0) {
            // Only the furthest intersection is after the start, so use it
            t = tMax;
            normal = normalMax;
        } else {
            // Both are after the start, use the closest one
            t = tMin;
            normal = normalMin;
        }
        normal = normal.normalize();
        // To avoid rounding point errors leaving the intersection point just off the plane
        // we check the normal to use the actual plane value from the box coordinates
        final double x;
        final double y;
        final double z;
        if (normal.getX() > 0) {
            x = this.max.getX();
        } else if (normal.getX() < 0) {
            x = this.min.getX();
        } else {
            x = direction.getX() * t + start.getX();
        }
        if (normal.getY() > 0) {
            y = this.max.getY();
        } else if (normal.getY() < 0) {
            y = this.min.getY();
        } else {
            y = direction.getY() * t + start.getY();
        }
        if (normal.getZ() > 0) {
            z = this.max.getZ();
        } else if (normal.getZ() < 0) {
            z = this.min.getZ();
        } else {
            z = direction.getZ() * t + start.getZ();
        }
        return Optional.of(new IntersectionPoint(new Vector3d(x, y, z), normal));
    }

    /**
     * Offsets this bounding box by a given amount and returns a new box.
     *
     * @param offset The offset to apply
     * @return The new offset box
     */
    public AABB offset(Vector3i offset) {
        checkNotNull(offset, "offset");
        return offset(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Offsets this bounding box by a given amount and returns a new box.
     *
     * @param offset The offset to apply
     * @return The new offset box
     */
    public AABB offset(Vector3d offset) {
        checkNotNull(offset, "offset");
        return offset(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Offsets this bounding box by a given amount and returns a new box.
     *
     * @param x The amount of offset for the x coordinate
     * @param y The amount of offset for the y coordinate
     * @param z The amount of offset for the z coordinate
     * @return The new offset box
     */
    public AABB offset(double x, double y, double z) {
        return new AABB(this.min.add(x, y, z), this.max.add(x, y, z));
    }

    /**
     * Expands this bounding box by a given amount in both directions and
     * returns a new box. The expansion is applied half and half to the
     * minimum and maximum corners.
     *
     * @param amount The amount of expansion to apply
     * @return The new expanded box
     */
    public AABB expand(Vector3i amount) {
        checkNotNull(amount, "amount");
        return expand(amount.getX(), amount.getY(), amount.getZ());
    }

    /**
     * Expands this bounding box by a given amount in both directions and
     * returns a new box. The expansion is applied half and half to the
     * minimum and maximum corners.
     *
     * @param amount The amount of expansion to apply
     * @return The new expanded box
     */
    public AABB expand(Vector3d amount) {
        checkNotNull(amount, "amount");
        return expand(amount.getX(), amount.getY(), amount.getZ());
    }

    /**
     * Expands this bounding box by a given amount in both directions and
     * returns a new box. The expansion is applied half and half to the
     * minimum and maximum corners.
     *
     * @param x The amount of expansion for the x coordinate
     * @param y The amount of expansion for the y coordinate
     * @param z The amount of expansion for the z coordinate
     * @return The new expanded box
     */
    public AABB expand(double x, double y, double z) {
        x /= 2;
        y /= 2;
        z /= 2;
        return new AABB(this.min.sub(x, y, z), this.max.add(x, y, z));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AABB)) {
            return false;
        }
        final AABB aabb = (AABB) other;
        return this.min.equals(aabb.min) && this.max.equals(aabb.max);

    }

    @Override
    public int hashCode() {
        int result = this.min.hashCode();
        result = 31 * result + this.max.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AABB(" + this.min + " to " + this.max + ")";
    }

}
