class FinalQuest{
    public static void main(String[] args) {
        BarcodeScanner im = new BarcodeScanner();
        im.read("images/Mission_5.bmp");
        im.convertTobinary();
        im.findBarcode();
        im.scanCode().forEach((key, value) -> { 
            System.out.println("------------------------------------------------------------------");
            System.out.println("Barcode found: "+key);
            System.out.println("Decoded character is "+value);
        });
    }
}