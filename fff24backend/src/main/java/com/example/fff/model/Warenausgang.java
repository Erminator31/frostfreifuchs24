            package com.example.fff.model;

            import com.example.fff.api.WarenausgangInterface;

            import java.util.List;

            /**
             * The Warenausgang class represents a shipping process in an inventory or warehouse system.
             * It encapsulates the details of a specific outbound shipment, including its identifier,
             * the date when the shipment is processed, and a list of items associated with the shipment.
             *
             * This class implements the WarenausgangInterface to ensure a standard structure for accessing
             * shipment details.
             */
            public class Warenausgang implements WarenausgangInterface {
                private int warenausgangId;
                private String warenausgangDate;
                private List<WarenausgangItem> items;

                public Warenausgang(int warenausgangId, String warenausgangDate, List<WarenausgangItem> items) {
                    this.warenausgangId = warenausgangId;
                    this.warenausgangDate = warenausgangDate;
                    this.items = items;
                }
                @Override
                public int getWarenausgangId() {
                    return warenausgangId;
                }
                @Override
                public String getWarenausgangDate() {
                    return warenausgangDate;
                }

                @Override
                public List<WarenausgangItem> getItems() {
                    return items;
                }
            }
