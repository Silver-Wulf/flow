/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.demo.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasSize;
import com.vaadin.ui.HasStyle;
import com.vaadin.util.JsonSerializer;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side component for the {@code <vaadin-grid>} element.
 *
 * @param <T>
 *            type of the items this grid displays
 */
@Tag("simple-grid")
@HtmlImport("frontend://src/simple-grid.html")
public class Grid<T> extends Component
        implements HasStyle, HasSize {

    /**
     * POC data provider interface.
     * 
     * @param <T>
     *            type of the items provided
     */
    public static interface SimpleDataProvider<T> {

        /**
         * The total number of available data items.
         * 
         * @return the total number of available data items
         */
        int size();

        /**
         * 
         * @param page
         * @param pageSize
         * @return
         */
        Stream<T> fetch(int page, int pageSize);

        /**
         * 
         * @param sizeCallback
         * @param fetchCallback
         * @return
         */
        public static <T> SimpleDataProvider<T> fromCallbacks(
                Supplier<Integer> sizeCallback,
                BiFunction<Integer, Integer, Stream<T>> fetchCallback) {
            return new SimpleDataProvider<T>() {

                @Override
                public int size() {
                    return sizeCallback.get();
                }

                @Override
                public Stream<T> fetch(int page, int pageSize) {
                    return fetchCallback.apply(page, pageSize);
                }
            };
        }
    }

    /**
     * POC in-memory data provider implementation.
     * 
     * @param <T>
     *            type of the items provided
     */
    public static class ListDataProvider<T> implements SimpleDataProvider<T> {
        
        private final List<T> items;
        
        public ListDataProvider(List<T> items) {
            this.items = items;
        }

        @Override
        public int size() {
            return items.size();
        }

        @Override
        public Stream<T> fetch(int page, int pageSize) {
            return items.subList(page * pageSize, page * pageSize + pageSize).stream();
        }
    }

    /**
     * Server-side component for the {@code <vaadin-grid-column>} element.
     * 
     * @param <T>
     *            type of the underlying grid this column is compatible with
     * @param <V>
     *            the item type this column displays
     */
    @Tag("vaadin-grid-column")
    @HtmlImport("frontend://bower_components/vaadin-grid/vaadin-grid-column.html")
    public static class Column<T, V> extends Component {

        private final Function<T, V> valueProvider;
        private final String columnId;

        private Element headerElement;
        private Element footerElement;

        /**
         * 
         * @param valueProvider
         */
        public Column(Function<T, V> valueProvider) {
            this.valueProvider = valueProvider;
            columnId = UUID.randomUUID().toString();
            getElement().appendChild(getTemplateElement());
        }

        private Element getTemplateElement() {
            Element template = new Element("template");
            template.setProperty("innerHTML",
                    String.format("[[item.%s]]", getColumnId()));
            return template;
        }

        /**
         * Gets the value provider of this column.
         * 
         * @return the value provider of this column.
         */
        public Function<T, V> getValueProvider() {
            return valueProvider;
        }

        /**
         * 
         * @return
         */
        public String getColumnId() {
            return columnId;
        }

        /**
         * Set the header template of this column.
         * 
         * @param headerTemplate
         *            the template to set
         * @return this column, for method chaining
         */
        public Column<T, V> setHeaderTemplate(String headerTemplate) {
            if (headerElement == null) {
                headerElement = new Element("template");
                headerElement.getClassList().add("header");
                getElement().appendChild(headerElement);
            }
            headerElement.setProperty("innerHTML", headerTemplate);
            return this;
        }

        /**
         * Set the footer template of this column.
         * 
         * @param footerTemplate
         *            the template to set
         * @return this column, for method chaining
         */
        public Column<T, V> setFooterTemplate(String footerTemplate) {
            if (footerElement == null) {
                footerElement = new Element("template");
                footerElement.getClassList().add("footer");
                getElement().appendChild(footerElement);
            }
            footerElement.setProperty("innerHTML", footerTemplate);
            return this;
        }
    }

    private final List<Column<T, ?>> columns;

    private SimpleDataProvider<T> dataProvider;

    /**
     * Default constructor.
     */
    public Grid() {
        columns = new ArrayList<>();
        getElement().addEventListener("page-request", this::requestPage,
                "event.detail.page", "event.detail.pageSize");
    }

    /**
     * Constructs a grid with the given data provider.
     * 
     * @see #setDataProvider(SimpleDataProvider)
     * 
     * @param dataProvider
     *            the data provider, not {@code null}
     */
    public Grid(SimpleDataProvider<T> dataProvider) {
        this();
        setDataProvider(dataProvider);
    }

    /**
     * Sets the data provider for this component. The data provider is queried
     * for displayed items as needed.
     * 
     * @param dataProvider
     *            the data provider, not {@code null}
     */
    public void setDataProvider(SimpleDataProvider<T> dataProvider) {
        Objects.requireNonNull(dataProvider,
                "The given data provider cannot be null");
        this.dataProvider = dataProvider;
        getElement().setProperty("size", this.dataProvider.size());
        getElement().callFunction("initDataProvider");
    }

    /**
     * Sets the data items of this component provided as a collection.
     * 
     * @param items
     *            the data items to display, not {@code null}
     */
    public void setItems(final Collection<T> items) {
        Objects.requireNonNull(items, "Given items cannot be null");
        setDataProvider(new ListDataProvider<>(new ArrayList<>(items)));
    }

    /**
     * Adds a new text column to this {@link Grid} with a value provider.
     * 
     * @param valueProvider
     *            the value provider
     * @return the new column
     */
    public <V> Column<T, V> addColumn(Function<T, V> valueProvider) {
        Column<T, V> column = new Column<>(valueProvider);
        column.getElement().setAttribute("slot", "columns");
        getElement().appendChild(column.getElement());
        columns.add(column);
        return column;
    }

    private void requestPage(DomEvent event) {
        int page = (int) event.getEventData().getNumber("event.detail.page");
        int pageSize = (int) event.getEventData()
                .getNumber("event.detail.pageSize");
        AtomicInteger index = new AtomicInteger();
        JsonArray items = dataProvider.fetch(page, pageSize)
                .map(this::itemToJson).collect(() -> Json.createArray(),
                        (arr, value) -> arr.set(index.getAndIncrement(), value),
                        (arr, arrOther) -> {
                            int startIndex = arr.length();
                            for (int i = 0; i < arrOther.length(); ++i) {
                                JsonValue value = arrOther.get(i);
                                arr.set(startIndex + i, value);
                            }
                        });
        JsonObject pageInfo = Json.createObject();
        pageInfo.put("page", page);
        pageInfo.put("items", items);
        getElement().callFunction("loadPage", pageInfo);
    }

    private JsonValue itemToJson(T item) {
        JsonObject value = Json.createObject();
        columns.forEach(column ->
            value.put(column.getColumnId(), JsonSerializer
                .toJson(column.getValueProvider().apply(item))));
        return value;
    }
}