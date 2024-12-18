package com.example.fff.model;
import java.util.List;
import com.example.fff.api.WareneingangInterface;

public class Wareneingang implements WareneingangInterface {


        private int wareneingangId;
        private String wareneingangDate;
        private List<WareneingangItem> items;

        public Wareneingang(int wareneingangId, String wareneingangDate, List<WareneingangItem> items) {
            this.wareneingangId = wareneingangId;
            this.wareneingangDate = wareneingangDate;
            this.items = items;
        }
@Override
public int getWareneingangId() {
            return wareneingangId;
        }
@Override
public String getWareneingangDate() {
            return wareneingangDate;
        }

        @Override
        public List<WareneingangItem> getItems() {
            return items;
        }

}
