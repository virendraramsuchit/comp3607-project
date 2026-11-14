package com.comp3607_project;

public class EditCalculator {

    public boolean isChanged(String oldText, String newText){

        if(oldText.equals(newText)){
            return false;
        }

        return true;
    }
    
    //Levenshtein distance using dynamic programming
    //Source: GeeksforGeeks, "Java Program to Implement Wagner and Fisher Algorithm for Online String Matching", https://www.geeksforgeeks.org/java/java-program-to-implement-wagner-and-fisher-algorithm-for-online-string-matching/
    public static int getDistance(String str1, String str2){

        int l1 = str1.length();
        int l2 = str2.length();

        if(l1 == 0){
            return l2;
        }

        if(l2 == 0){
            return l1;
        }

        int arr[][] = new int[l1 + 1][l2 + 1];

        for(int i = 0; i <= l1; i++){
            arr[i][0] = i;
        }

        for(int j = 0; j <= l2; j++){
            arr[0][j] = j;
        }

        for(int i = 1; i <= l1; i++){
            char ch1 = str1.charAt(i - 1);

            for(int j = 1; j <= l2; j++){
                char ch2 = str2.charAt(j - 1);

                int m = ch1 == ch2 ? 0 : 1;

                arr[i][j] = Math.min(
                    Math.min((arr[i - 1][j] + 1),
                            (arr[i][j - 1] + 1)),
                    arr[i - 1][j - 1] + m);
            }
        }

        return arr[l1][l2];
    }

    //Addition: skip the start and end of the text that did not change, so the table above is only built for
    //the part that was edited. Without this, large files need too much memory
    public static int levenshtein(String oldText, String newText){

        int max = Math.min(oldText.length(), newText.length());
        int start = 0;
        while(start < max && oldText.charAt(start) == newText.charAt(start)){
            start++;
        }

        int endOld = oldText.length();
        int endNew = newText.length();
        while(endOld > start && endNew > start && oldText.charAt(endOld - 1) == newText.charAt(endNew - 1)){
            endOld--;
            endNew--;
        }

        return getDistance(oldText.substring(start, endOld), newText.substring(start, endNew));
    }

    public int getNumChanges(String oldText, String newText){

        if(!isChanged(oldText, newText)){
            return 0; 
        }

        return levenshtein(oldText, newText);
    }
}

