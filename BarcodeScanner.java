import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.lang.Math;


class BarcodeScanner {
    public int width;
    public int height;
    public int bitdept;

    public String foundcode;
    public String[][] barcodetable;
    private List<Integer> codewidth;
    private List<Integer> codecolor;
    private Map<String, Integer> codecount;

    private BufferedImage img;
    private BufferedImage origin;
    public boolean read(String fileName){
        File f = null;
        f = new File(fileName);
        try {
            img = ImageIO.read(f);
            width = img.getWidth();
            height = img.getHeight();
            bitdept = img.getColorModel().getPixelSize(); 


            origin = new BufferedImage(width,height, img.getType());
            for (int y =0; y < height; y++){
                for(int x =0; x< width; x++){
                    origin.setRGB(x,y, img.getRGB(x, y));
                }

            }
            return true;
        } catch (IOException e) {
            //TODO: handle exception
            return false;
        }
        

    }
    public boolean write(String fileName){
        try {
            ImageIO.write(img, "bmp", new File(fileName));
            return true;
        } catch (IOException e) {
            //TODO: handle exception
            return false;
        }
        catch(NullPointerException e){
            return false;
        }
    }
    public void restore(){
        for(int y = 0; y< height ;y++){
            for(int x = 0; x < width ;x++){
                img.setRGB(x,y , origin.getRGB(x,y));
            }
        }
    }

    public void findBarcode(){
        if(img == null) return;

        codewidth = new ArrayList<>();
        codecolor = new ArrayList<>();
        codecount = new HashMap<String, Integer>();
        for(int y =0;y < height; y++)
        {
            int black = 0;
            int white = 0;
            for(int x =0; x< width;x++)
            {
                int color = img.getRGB(x, y);
                int r = (color >> 16)& 0xff;
                int g = (color >> 8)& 0xff;
                int b = color & 0xff;
                int binary = (r+g+b)/3;
                switch(binary){
                    case 0:
                    if(white > 0){
                        white = averageColorWidth(white);//Average of barcode width is 5 px per unit
                        codewidth.add(white);
                        codecolor.add(0);
                        white = 0;
                    }
                    black++;
                    break;
                        
                    case 255:
                    if(black > 0){
                        black = averageColorWidth(black);
                        codewidth.add(black);
                        codecolor.add(1);
                        black = 0;
                    }
                    white++;
                    break;
                }
            }
            if(white > 0){
                white = averageColorWidth(white);
                codewidth.add(white);
                codecolor.add(0);
                white = 0;
            }
            else if(black > 0){
                black = averageColorWidth(black);
                codewidth.add(black);
                codecolor.add(1);
                black = 0;
            }

            if (codecolor.size() > 6)//Barcode strip should have more than 6 strip
            {
                StringBuilder code = new StringBuilder();
                boolean x = false;
                for(int i = 0; i < codewidth.size() - 3; i++)
                {
                    code.setLength(0);
                    if (codewidth.get(i) == 10 && codecolor.get(i) == 1)
                    {
                        if (codewidth.get(i + 1) == 5 && codecolor.get(i + 1) == 0)
                        {
                            if (codewidth.get(i + 2) >= 5 && codecolor.get(i + 2) == 1)
                            {
                                code.append("1101");
                                if(codewidth.get(i + 2) > 5 && codecolor.get(i + 2) == 1)
                                {
                                    for (int c = 1; c <  codewidth.get(i + 2) / 5; c++)
                                        code.append("1");
                                }
                                for(int c = i+3; c < codewidth.size(); c++)
                                {
                                     x = true;
                                    if(!(codewidth.get(c) == 5 || codewidth.get(c) == 10 || codewidth.get(c) == 15))
                                    {
                                        break;
                                    }
                                    switch(codewidth.get(c))
                                    {
                                        case 5:
                                        if(codecolor.get(c) == 0){
                                            code.append("0");
                                        }
                                            
                                        else if(codecolor.get(c) == 1){
                                            code.append("1");
                                        }
                                            
                                        break;

                                        case 10:
                                        if(codecolor.get(c) == 0){
                                            code.append("00");
                                        }
                                            
                                        else if(codecolor.get(c) == 1){
                                            code.append("11");
                                        }
                                        break;

                                        case 15:
                                        if(codecolor.get(c) == 0){
                                            code.append("000");
                                        }
                                            
                                        else if(codecolor.get(c) == 1){
                                            code.append("111");
                                        }
                                        break;
                                    }
                                    
                                }
                                if (code.length() > 8)//Barcode are should more than 8 character
                                {

                                    StringBuilder stopcode = new StringBuilder();
                                    stopcode.append(code.charAt(code.length() - 4));
                                    stopcode.append(code.charAt(code.length() - 3));
                                    stopcode.append(code.charAt(code.length() - 2));
                                    stopcode.append(code.charAt(code.length() - 1));
                                    
                                    if (stopcode.toString().equals("1011"))
                                    {
                                        if(codecount.size() == 0)
                                            codecount.put(code.toString(), 1);
                                        else
                                        {
                                            List<String> keys = new ArrayList<String>(codecount.keySet());
                                            boolean found = false;
                                            for (int m = 0; m < keys.size(); m++) {
                                                if(keys.get(m).toString().equals(code.toString()))
                                                {
                                                    codecount.put(code.toString(), codecount.get(code.toString()) + 1);
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if(!found){
                                                codecount.put(code.toString(), 1);
                                            }
                                        }
                                    } 
                                }
                            }
                        }
                    } 
                }
            }
            codewidth.removeAll(codewidth);
            codecolor.removeAll(codecolor); 
        }
        codecount = sortByValue(codecount);
    }

    public void scanCode()
    {
        barcodeTable();
        StringBuilder decodechar = new StringBuilder("");
        codecount.forEach((key, value) -> {         
            StringBuilder code = new StringBuilder(key);
            System.out.println("Possible code: "+code+" found: "+value+" size: "+code.length());   
            code.delete(0, 4);
            code.delete(code.length() - 4, code.length());
            if(code.length() % 11 == 0)
            {
                StringBuilder charcode = new StringBuilder("");
                for(int d = 0; d < code.length(); d++)
                {
                    
                    if(charcode.length() == 11)
                    {
                        boolean foundchar = false;
                        for(int i = 0; i< barcodetable.length; i++)
                        {
                            if(charcode.toString().equals(barcodetable[i][1]))
                            {
                                decodechar.append(barcodetable[i][0]);
                                foundchar = true;
                            }
                        }
                        if(!foundchar)
                        {
                            System.out.println("Find status:: "+foundchar);
                            decodechar.setLength(0);
                            break;
                        }
                        charcode.setLength(0);
                        charcode.append(code.charAt(d));
                        foundcode = code.toString();
                    }
                    else 
                    {
                        charcode.append(code.charAt(d));
                    }
                }
                System.out.println("Find status: true");
            }
            else System.out.println("Find status: false");
            System.out.println();
        
        });
        System.out.println("Barcode code is "+foundcode);
        System.out.println("GM Code is "+decodechar);
    }

    public int averageColorWidth(int colorwidth)
    {
        //Average of barcode width is 5 px per unit
        if(Math.abs(colorwidth - 5) <= 2){
            colorwidth = 5;
        }
        else if(Math.abs(colorwidth - 10) <= 2){
            colorwidth = 10;
        }
        else if(Math.abs(colorwidth - 15) <= 2){
            colorwidth = 15;
        }
        return colorwidth;
    }

    public void convertTobinary(){
        if(img == null) return;
        for(int y =0;y < height; y++){
            for(int x =0; x< width;x++){
                int color = img.getRGB(x, y);
                int r = (color >> 16)& 0xff;
                int g = (color >> 8)& 0xff;
                int b = color & 0xff;
                int binary = (r+g+b)/3;
                binary = binary < 127? 0: 255;
                color = (binary << 16) | (binary << 8) | binary;
                img.setRGB(x,y, color);
            }
        }
    }

    public Map<String, Integer> sortByValue(Map<String, Integer> wordCounts) {
        return wordCounts.entrySet()
            .stream()
            .sorted((Map.Entry.<String, Integer>comparingByValue().reversed()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public void barcodeTable(){
        barcodetable = new String[][]
        {
            {" ","11011001100"},//0
            {"!","11001101100"},//1
            {"\"","11001100110"},//2
            {"#","10010011000"},//3
            {"$","10010001100"},//4
            {"%","10001001100"},//5
            {"&","10011001000"},//6
            {"'","10011000100"},//7
            {"(","10001100100"},//8
            {")","11001001000"},//9
            {"*","11001000100"},//10
            {"+","11000100100"},//11
            {",","10110011100"},//12
            {"-","10011011100"},//13
            {".","10011001110"},//14
            {"/","10111001100"},//15
            {"0","10011101100"},//16
            {"1","10011100110"},//17
            {"2","11001110010"},//18
            {"3","11001011100"},//19
            {"4","11001001110"},//20
            {"5","11011100100"},//21
            {"6","11001110100"},//22
            {"7","11101101110"},//23
            {"8","11101001100"},//24
            {"9","11100101100"},//25
            {":","11100100110"},//26
            {";","11101100100"},//27
            {"<","11100110100"},//28
            {"=","11100110010"},//29
            {">","11011011000"},//30
            {"?","11011000110"},//31
            {"@","11000110110"},//22
            {"A","10100011000"},//33
            {"B","10001011000"},//34
            {"C","10001000110"},//35
            {"D","10110001000"},//36
            {"E","10001101000"},//37
            {"F","10001100010"},//38
            {"G","11010001000"},//39
            {"H","11000101000"},//40
            {"I","11000100010"},//41
            {"J","10110111000"},//42
            {"K","10110001110"},//43
            {"L","10001101110"},//44
            {"M","10111011000"},//45
            {"N","10111000110"},//46
            {"O","10001110110"},//47
            {"P","11101110110"},//48
            {"Q","11010001110"},//49
            {"R","11000101110"},//50
            {"S","11011101000"},//51
            {"T","11011100010"},//52
            {"U","11011101110"},//53
            {"V","11101011000"},//54
            {"W","11101000110"},//55
            {"X","11100010110"},//56
            {"Y","11101101000"},//57
            {"Z","11101100010"},//58
            {"[","11100011010"},//59
            {"\\","11101111010"},//60
            {"]","11001000010"},//61
            {"^","11110001010"},//62
            {"_","10100110000"},//63
        };
    }
}