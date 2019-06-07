package rmkl;

/*
    Base algorithm adapted from
    http://gamedevelopment.tutsplus.com/tutorials/how-to-use-tile-bitmasking-to-auto-tile-your-level-layouts--cms-25673
*/

public class Bitmask {

    public static int check4bit(int x, int y, char[][] map) {
        return check4bit(x, y, map, map[x][y]);
    }

    public static int check4bit(int x, int y, char[][] map, char... tileTypes) {

        boolean biggerThanLeft = false, smallerThanRight = false, biggerThanBottom = false, smallerThanTop = false;

        // Check map boundaries
        if(y+1 < map[x].length) smallerThanTop = true;
        if(x-1 >= 0) 			biggerThanLeft = true;
        if(x+1 < map.length)    smallerThanRight = true;
        if(y-1 >= 0) 			biggerThanBottom = true;

        int North = 0, West = 0, East = 0, South = 0;

        // Check neighbouring tiles
        if(smallerThanTop)   for(char c : tileTypes) if(map[x][y+1] == c) North = 1;
        if(biggerThanLeft)   for(char c : tileTypes) if(map[x-1][y] == c) West = 1;
        if(smallerThanRight) for(char c : tileTypes) if(map[x+1][y] == c) East = 1;
        if(biggerThanBottom) for(char c : tileTypes) if(map[x][y-1] == c) South = 1;

        return North + West*2 + East*4 + South*8;
    }

    public static int check8bit(int x, int y, char[][] map) {
        return check8bit(x, y, map, map[x][y]);
    }

    public static int check8bit(int x, int y, char[][] map, char... tileTypes) {

        boolean biggerThanLeft = false, smallerThanRight = false, biggerThanBottom = false, smallerThanTop = false;

        // Check map boundaries
        if(y+1 < map[x].length) smallerThanTop = true;
        if(x-1 >= 0) 			biggerThanLeft = true;
        if(x+1 < map.length) 	smallerThanRight = true;
        if(y-1 >= 0) 			biggerThanBottom = true;

        int North = 0, West = 0, East = 0, South = 0;

        // Check neighbouring tiles
        if(smallerThanTop)   for(char c : tileTypes) if(map[x][y+1] == c) North = 1;
        if(biggerThanLeft)   for(char c : tileTypes) if(map[x-1][y] == c) West = 1;
        if(smallerThanRight) for(char c : tileTypes) if(map[x+1][y] == c) East = 1;
        if(biggerThanBottom) for(char c : tileTypes) if(map[x][y-1] == c) South = 1;

        int NorthWest = 0, NorthEast = 0, SouthWest = 0, SouthEast = 0;

        // Check corners only if neighbouring tiles are 1
        if(North == 1 && West == 1) for(char c : tileTypes) if(map[x-1][y+1] == c) NorthWest = 1;
        if(North == 1 && East == 1) for(char c : tileTypes) if(map[x+1][y+1] == c) NorthEast = 1;
        if(South == 1 && West == 1) for(char c : tileTypes) if(map[x-1][y-1] == c) SouthWest = 1;
        if(South == 1 && East == 1) for(char c : tileTypes) if(map[x+1][y-1] == c) SouthEast = 1;

        // Return bitmask
        return NorthWest + North*2 + NorthEast*4 + West*8 + East*16 + SouthWest*32 + South*64 + SouthEast*128;
    }

    /**
     * @return the id of the tile in the tileset image [0-47]
     */
    public static int rawTile8bit(int bitmask) {
        switch(bitmask) {
            case   2: return  1;
            case   8: return  2;
            case  10: return  3;
            case  11: return  4;
            case  16: return  5;
            case  18: return  6;
            case  22: return  7;
            case  24: return  8;
            case  26: return  9;
            case  27: return 10;
            case  30: return 11;
            case  31: return 12;
            case  64: return 13;
            case  66: return 14;
            case  72: return 15;
            case  74: return 16;
            case  75: return 17;
            case  80: return 18;
            case  82: return 19;
            case  86: return 20;
            case  88: return 21;
            case  90: return 22;
            case  91: return 23;
            case  94: return 24;
            case  95: return 25;
            case 104: return 26;
            case 106: return 27;
            case 107: return 28;
            case 120: return 29;
            case 122: return 30;
            case 123: return 31;
            case 126: return 32;
            case 127: return 33;
            case 208: return 34;
            case 210: return 35;
            case 214: return 36;
            case 216: return 37;
            case 218: return 38;
            case 219: return 39;
            case 222: return 40;
            case 223: return 41;
            case 248: return 42;
            case 250: return 43;
            case 251: return 44;
            case 254: return 45;
            case 255: return 46;
            case   0: return 47;
        }

        return -1;
    }

    /**
     * @return the id of the id prepared to be fetched from the model. Is the archetipe of its group (color) in the tileset image
     */
    public static int baseTile8bit(int bitmask) {
        switch(bitmask) {
            case  0: return  0;
            case  1: return  1;
            case  2: return  1;
            case  3: return  3;
            case  4: return  4;
            case  5: return  1;
            case  6: return  3;
            case  7: return  4;
            case  8: return 14;
            case  9: return 16;
            case 10: return 27;
            case 11: return 17;
            case 12: return 28;
            case 13: return  1;
            case 14: return 14;
            case 15: return  3;
            case 16: return 16;
            case 17: return 17;
            case 18: return  3;
            case 19: return 16;
            case 20: return 27;
            case 21: return 16;
            case 22: return 22;
            case 23: return 30;
            case 24: return 30;
            case 25: return 31;
            case 26: return  4;
            case 27: return 27;
            case 28: return 28;
            case 29: return 17;
            case 30: return 30;
            case 31: return 31;
            case 32: return 32;
            case 33: return 33;
            case 34: return  4;
            case 35: return 17;
            case 36: return 28;
            case 37: return 27;
            case 38: return 30;
            case 39: return 32;
            case 40: return 31;
            case 41: return 33;
            case 42: return 28;
            case 43: return 31;
            case 44: return 33;
            case 45: return 33;
            case 46: return 46;
            case 47: return 47;
        }

        return -1;
    }

    /**
     * @param rawTile obtained from Bitmask#rawTile8bit()
     * @return the rotation in degrees for ModelInstance#transform#setToRotation(Vector3.Y, degrees)
     */
    public static int tileRotation8bit(int rawTile) {
        switch(rawTile) {
            case  5: case  6: case  7: case  8: case  9:
            case 10: case 11: case 12: case 23: case 25:
            case 39: case 41:
                return 90;

            case 13: case 18: case 19: case 20: case 24:
            case 34: case 35: case 36: case 40: case 45:
                return 180;

            case  2: case 15: case 21: case 26: case 29:
            case 37: case 38: case 42: case 43: case 44:
                return 270;
        }

        return 0;
    }
}
