import java.util.ArrayList;
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
    private int width;
    private int height;
    private int bitdept;

    private String[][] barcodetable;
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
    public void restore()
    {
        for(int y = 0; y< height ;y++){
            for(int x = 0; x < width ;x++){
                img.setRGB(x,y , origin.getRGB(x,y));
            }
        }
    }

    public void findBarcode()
    {
        if(img == null) return;
        
        codecount = new HashMap<String, Integer>();
        for(int y =0;y < height; y++)
        {
            List<Integer> codewidth = new ArrayList<>();
            List<Integer> codecolor = new ArrayList<>();
            int blackwidth = 0;
            int whitewidth = 0;
            for(int x =0; x< width;x++)//find color width from image
            {
                int color = img.getRGB(x, y);
                int r = (color >> 16)& 0xff;
                int g = (color >> 8)& 0xff;
                int b = color & 0xff;
                int binary = (r+g+b)/3;
                switch(binary)
                {
                    case 0://if color is black
                    if(whitewidth > 0)//add code width of white color after found black color
                    {
                        whitewidth = averageColorWidth(whitewidth);//Average of barcode width is 5 px per unit
                        codewidth.add(whitewidth);//add code width
                        codecolor.add(0);//add code color
                        whitewidth = 0;
                    }
                    blackwidth++;
                    break;
                        
                    case 255://if color is white
                    if(blackwidth > 0)//add code width of black color after found white color
                    {
                        blackwidth = averageColorWidth(blackwidth);//Average of barcode width is 5 px per unit
                        codewidth.add(blackwidth);//add code width
                        codecolor.add(1);//add code color
                        blackwidth = 0;
                    }
                    whitewidth++;
                    break;
                }
            }
            if(whitewidth > 0)
            {
                whitewidth = averageColorWidth(whitewidth);//Average of barcode width is 5 px per unit
                codewidth.add(whitewidth);//add code width
                codecolor.add(0);//add code color
                whitewidth = 0;
            }
            else if(blackwidth > 0)
            {
                blackwidth = averageColorWidth(blackwidth);//Average of barcode width is 5 px per unit
                codewidth.add(blackwidth);//add code width
                codecolor.add(1);//add code color
                blackwidth = 0;
            }

            if (codecolor.size() > 6)//Barcode strip should more than 6 strip
            {
                StringBuilder code = new StringBuilder();
                for(int i = 0; i < codewidth.size() - 6; i++)//find barcode from arraylist
                {
                    code.setLength(0);

                    if (codewidth.get(i) == 10 && codecolor.get(i) == 1
                        && codewidth.get(i + 1) == 5 && codecolor.get(i + 1) == 0
                        && codewidth.get(i + 2) >= 5 && codecolor.get(i + 2) == 1)//if have start code
                    {
                    
                        code.append("1101");

                        if(codewidth.get(i + 2) > 5 && codecolor.get(i + 2) == 1)//if have black strip after start code
                        {
                            for (int c = 1; c <  codewidth.get(i + 2) / 5; c++)
                                code.append("1");
                        }

                        for(int c = i+3; c < codewidth.size(); c++)//convert width to code128 
                        {
                            if(!(codewidth.get(c) == 5 || codewidth.get(c) == 10 || codewidth.get(c) == 15))//if not a barcode width
                            {
                                break;//stop convert color width to code128
                            }

                            switch(codewidth.get(c))
                            {
                                case 5://if color width is 5
                                if(codecolor.get(c) == 0){//if color is white
                                    code.append("0");
                                }
                                else if(codecolor.get(c) == 1){//if color is black
                                    code.append("1");
                                }
                                break;

                                case 10://if color width is 10
                                if(codecolor.get(c) == 0){//if color is white
                                    code.append("00");
                                }
                                else if(codecolor.get(c) == 1){//if color is black
                                    code.append("11");
                                }
                                break;

                                case 15://if color width is 15
                                if(codecolor.get(c) == 0){//if color is white
                                    code.append("000");
                                }
                                else if(codecolor.get(c) == 1){//if color is black
                                    code.append("111");
                                }
                                break;
                            }
                            
                        }

                        if (code.length() > 8)//code character should more than 8 character
                        {
                            StringBuilder stopcode = new StringBuilder();

                            //read stop code
                            stopcode.append(code.charAt(code.length() - 4));
                            stopcode.append(code.charAt(code.length() - 3));
                            stopcode.append(code.charAt(code.length() - 2));
                            stopcode.append(code.charAt(code.length() - 1));
                            
                            if (stopcode.toString().equals("1011"))//if have stop code
                            {
                                if(codecount.size() == 0)//if not have code put to map function
                                    codecount.put(code.toString(), 1);
                                else
                                {
                                    List<String> keys = new ArrayList<String>(codecount.keySet());
                                    boolean found = false;

                                    for (int m = 0; m < keys.size(); m++)//find same code in map function
                                    {
                                        if(keys.get(m).toString().equals(code.toString()))//if found same code in map function
                                        {
                                            codecount.put(code.toString(), codecount.get(code.toString()) + 1);//add number of code was found
                                            found = true;
                                            break;
                                        }
                                    }

                                    if(!found)//if not found same code in map function
                                    {
                                        codecount.put(code.toString(), 1);//put code to map function
                                    }
                                }
                            } 
                        }
                    } 
                }
            }
        }

        codecount = sortByValue(codecount);//sort by maximum number of code was found
    }

    public Map<String,String> scanCode()
    {
        barcodeTable();//create code data table
        StringBuilder decodedchar = new StringBuilder("");
        Map<String,String> decodedlist = new HashMap<String,String>();//for contain all decoded character from code128

        codecount.forEach((key, value) -> {//decode code128         
            StringBuilder code = new StringBuilder(key);
            System.out.println("Possible code: "+code+" found: "+value+" size: "+code.length());   
            code.delete(0, 4);//cut start code
            code.delete(code.length() - 4, code.length());//cut stop code

            if(code.length() % 11 == 0)//code length must divide with 11 without fraction
            {
                StringBuilder charcode = new StringBuilder("");
                
                for(int d = 0; d < code.length(); d++)//read code from map function
                {
                    
                    if(charcode.length() == 11)
                    {
                        boolean foundchar = false;

                        for(int i = 0; i< barcodetable.length; i++)//find character from table
                        {
                            if(charcode.toString().equals(barcodetable[i][1]))//if found character from table
                            {
                                decodedchar.append(barcodetable[i][0]);
                                foundchar = true;
                            }
                        }

                        if(!foundchar)//if not found character from table
                        {
                            decodedchar.setLength(0);
                            break;//cancel decode code128
                        }

                        charcode.setLength(0);
                        charcode.append(code.charAt(d));
                        decodedlist.put(key, decodedchar.toString());//put decoded character from code128 to map function
                    }
                    else
                    {
                        charcode.append(code.charAt(d));
                    }
                }
            }
        });

        return decodedlist;
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

    public void medianFilter(int size)
    {
        if (img == null) return;
        if (size % 2 == 0)
        {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }
        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int[] red = new int[size*size];
                int[] green = new int[size*size];
                int[] blue = new int[size*size];
                int redMedian = 0;
                int greenMedian = 0;
                int blueMedian = 0;
                int count = 0;
                for (int i = y - size/2; i <= y + size/2; i++)
                {
                    for (int j = x - size/2; j <= x + size/2; j++)
                    {
                        if (i >= 0 && i < height && j >= 0 && j < width)
                        {
                            int color = img.getRGB(j, i);
                            int r = (color >> 16) & 0xff;
                            int g = (color >> 8) & 0xff;
                            int b = color & 0xff;
                            red[count] = r;
                            green[count] = g;
                            blue[count] = b;
                            count++;
                        }
                    }
                }
                java.util.Arrays.sort(red);
                java.util.Arrays.sort(green);
                java.util.Arrays.sort(blue);
                redMedian = red[red.length/2];
                greenMedian = green[green.length/2];
                blueMedian = blue[blue.length/2];
                int newColor = (redMedian << 16) | (greenMedian << 8) | blueMedian;
                tempBuf.setRGB(x, y, newColor);
            }
        }
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    //sort function
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