import java.util.Scanner;

class FinalQuest{
    public static void main(String[] args) {
        BarcodeScanner im = new BarcodeScanner();
        
        im.read("images/Mission_5.bmp");

        im.convertTobinary();
        im.write("images/binary1.bmp");
        im.findBarcode();
        im.scanCode();
    }
}