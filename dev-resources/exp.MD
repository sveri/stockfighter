## Assumption
If an insider has some information other traders do not have, then the information has value only, if it is 
acted upon before it becomes public.  
What I did was to compare the actual NAVs versus NAVs x trading days, if the account would have traded without the information.

## Actual data

This is the data gathered after approximately 15 minutes:  

account - nav - nav-10 diff bids and asks total  

     {"HAC58513641" [19553.42 -14108.64 3366206 66 72],
     "JAH62387436" [16365.01 -19094.4 3545941 58 79],
     "LIS17349736" [15344.34 -21249.3 3659364 63 52],
     "AS35244724" [12579.31 -8276.16 2085547 24 4],
     "SAC77294558" [11133.34 -20893.82 3202716 25 34],
     "KAM94760806" [10766.35 -20772.2 3153855 33 47],
     "LFB86246389" [9594.42 -10122.24 1971666 26 34],
     "LPB97468088" [7901.0 -17980.56 2588156 60 75],
     "FMB88300345" [5353.97 -16160.88 2151485 35 47],
     "HMY83653718" [2131.32 -1782.72 391404 4 18],
     "SLB60824604" [1755.14 -14493.6 1624874 7 43],
     "ELS8464452" [1686.76 -17765.05 1945181 26 30],
     "DOB50647902" [48.19 -6137.76 618595 6 3]}
 
You can see that there is one outlier:  
"AS35244724" [12579.31 -8276.16 2085547 24 4]  
with very few trades, a reasonable net win and a reasonable loss (20855.47) if he would have traded later.
