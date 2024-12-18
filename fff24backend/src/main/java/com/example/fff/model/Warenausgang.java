    package com.example.fff.model;

    import com.example.fff.api.WarenausgangInterface;

    import java.util.List;

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
