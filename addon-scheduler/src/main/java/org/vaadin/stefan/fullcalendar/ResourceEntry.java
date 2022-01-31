/*
 * Copyright 2020, Stefan Uebe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.vaadin.stefan.fullcalendar;

import elemental.json.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Represents an entry that can be connected with a resource. Needed for timeline views.
 */
@Getter
@Setter
public class ResourceEntry extends Entry {

    private Set<Resource> resources;

    /**
     * Defines, if the user can move entries between resources (by using drag and drop). This value
     * is passed to the client side and interpreted there, but can also be used for server side checks.
     * <br><br>
     * This value has no impact on the resource API of this class.
     */
    private boolean resourceEditable;

    /**
     * Creates a new entry with a generated id.
     */
    public ResourceEntry() {
    }

    /**
     * Creates a new entry with the given id. Null will lead to a generated id.
     * <br><br>
     * Please be aware, that the ID needs to be unique in the calendar instance. Otherwise it can lead to
     * unpredictable results.
     *
     * @param id id
     */
    public ResourceEntry(String id) {
        super(id);
    }

    /**
     * Sets the calendar for this instance. The given calendar must be implementing Scheduler.
     *
     * @param calendar calendar instance
     * @throws IllegalArgumentException instance is not implementing {@link Scheduler}
     */
    @Override
    protected void setCalendar(FullCalendar calendar) {
        if (calendar != null && !(calendar instanceof Scheduler)) {
            throw new IllegalArgumentException("ResourceEntries must be added to a FullCalendar that implements Scheduler");
        }
        super.setCalendar(calendar);
    }

    /**
     * Returns an assigned resource. Is empty if no resource has been assigned yet. This method is mainly intended
     * to be used for entries where it is sure, that it only has one resource. For entries with multiple
     * resources it might be, that on the next call, the resource change (currently this class uses
     * a {@link LinkedHashSet}, but that might change in future).
     *
     * @return resource
     */
    public Optional<Resource> getResource() {
        return getResourcesOrEmpty().stream().findFirst();
    }

    /**
     * Returns an unmodifiable set of resources or an empty one, if no resources have been defined yet.
     *
     * @return unmodifiable set of resources
     */
    public Set<Resource> getResourcesOrEmpty() {
        Set<Resource> resources = getResources();
        return resources != null ? Collections.unmodifiableSet(resources) : Collections.emptySet();
    }

    /**
     * Returns a copy of the entry's assigned resources. Any changes to this set are reflected to the
     * backend and will be applied to the client on the next entry's update.
     * <p></p>
     * In earlier versions this set might have been unmodifiable. This is not the case anymore.
     *
     * @return entry's resources
     */
    public Set<Resource> getResources() {
        if (resources == null) {
            resources = new LinkedHashSet<>();
        }

        return resources;
    }

    /**
     * Returns the amount of assigned resources.
     *
     * @return resources
     */
    public int getResourcesSize() {
        return getResourcesOrEmpty().size();
    }

    /**
     * Returns, if the entry has any ressources assigned.
     *
     * @return has resources
     */
    public boolean hasResources() {
        return !getResourcesOrEmpty().isEmpty();
    }

    /**
     * Assign an additional resource to this entry. Already assigned resources will be kept.
     *
     * @param resource resource
     * @throws NullPointerException when null is passed
     */
    public void assignResource(@NotNull Resource resource) {
        assignResources(Objects.requireNonNull(resource));
    }

    /**
     * Assign additional resources to this entry. Already assigned resources will be kept.
     *
     * @param resources resources
     * @throws NullPointerException when null is passed
     */
    public void assignResources(@NotNull Collection<Resource> resources) {
        Objects.requireNonNull(resources);
        getResources().addAll(resources);
    }

    /**
     * Assigns additional resources to this entry. Already assigned resources will be kept.
     *
     * @param resources resources
     * @throws NullPointerException when null is passed
     */
    public void assignResources(@NotNull Resource... resources) {
        assignResources(Arrays.asList(resources));
    }

    /**
     * Unassigns the given resource from this entry.
     *
     * @param resource resource
     * @throws NullPointerException when null is passed
     */
    public void unassignResource(@NotNull Resource resource) {
        unassignResources(Objects.requireNonNull(resource));
    }

    /**
     * Unassigns the given resources from this entry.
     *
     * @param resources resources
     * @throws NullPointerException when null is passed
     */
    public void unassignResources(@NotNull Resource... resources) {
        unassignResources(Arrays.asList(resources));
    }

    /**
     * Unassigns the given resources from this entry.
     *
     * @param resources resources
     * @throws NullPointerException when null is passed
     */
    public void unassignResources(@NotNull Collection<Resource> resources) {
        if (hasResources()) {
            getResources().removeAll(resources);
        }
    }

    /**
     * Unassigns all resources from this entry.
     */
    public void unassignAllResources() {
        if (this.resources != null) {
            this.resources.clear();
            this.resources = null;
        }
    }

    @Override
    protected JsonObject toJson() {
        JsonObject jsonObject = super.toJson();

        if (resources != null) {
            JsonArray array = Json.createArray();
            int i = 0;
            for (Resource r : resources) {
                array.set(i++, r.getId());
            }

            jsonObject.put("resourceIds", array);

            if (getColor() == null && !resources.isEmpty()) {
                jsonObject.put("_hardReset", true);  // currently needed to make sure, that the color is
                // set correctly. Might change in future, if not performant
            }
        }


        jsonObject.put("resourceEditable", isResourceEditable());

        return jsonObject;
    }

    @Override
    protected void update(JsonObject object) {
        super.update(object);

        getCalendar().map(c -> (Scheduler) c).ifPresent(calendar -> {
            Optional<Set<Resource>> oldResource = Optional.<JsonValue>ofNullable(object.get("oldResource"))
                    .filter(o -> o instanceof JsonString)
                    .map(JsonValue::asString)
                    .flatMap(calendar::getResourceById)
                    .map(Collections::singleton);
            oldResource
                    .ifPresent(this::unassignResources);

            Optional.<JsonValue>ofNullable(object.get("newResource"))
                    .filter(o -> o instanceof JsonString)
                    .map(JsonValue::asString)
                    .flatMap(calendar::getResourceById)
                    .map(Collections::singleton)
                    .ifPresent(this::assignResources);

        });
    }
}
