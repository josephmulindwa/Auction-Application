package model;

public class SearchFilter {
    private String name;
    private String title;
    private String category;
    private int priceMin;
    private int priceMax;
    private byte checkedCode;

    public SearchFilter(String name, String category, int priceMin, int priceMax){
        this.name = name;
        this.title = "";
        this.category = category;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        checkedCode = 0;
    }

    public String getName() {
        return name;
    }

    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }

    public void setName(String name) {
        this.name = name;
    }

    public final int getCheckedCode(){
        return checkedCode;
    }

    public final void setCheckedCode(byte checkedCode){
        this.checkedCode = checkedCode;
    }

    public int getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(int priceMin) {
        this.priceMin = priceMin;
    }

    public int getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(int priceMax) {
        this.priceMax = priceMax;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
    
    public void setIndexChecked(int i, boolean checked){
        if(checked) {
            checkedCode |= (1 << i);
        }else{
            checkedCode &= ~(1 << i);
        }
    }
    
    public boolean getIndexClicked(int i){
        int tmpc = (checkedCode >> i) & 1;
        return (tmpc == 1);
    }

}
