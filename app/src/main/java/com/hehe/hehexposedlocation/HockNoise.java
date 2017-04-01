package com.hehe.hehexposedlocation;

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.hehe.hehexposedlocation.FreeList.FREE_CATEGORY_LIST;
import static com.hehe.hehexposedlocation.FreeList.KEYWORD_LIST;
import static com.hehe.hehexposedlocation.FreeList.PACKAGE_LIST;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

/**
 * The Spinner noise setting
 */
public class HockNoise implements IXposedHookLoadPackage {

    private final static int sdk = Build.VERSION.SDK_INT;
    private final static double MaxLat = -90.0;
    private final static double MinLat = 90.0;
    private final static double MaxLong = 180.0;
    private final static double MinLong = -180.0;
    private final static String[] FreePackageList = PACKAGE_LIST;
    private final static String[] FreeKeywordList = KEYWORD_LIST;
    private final static String[] FreeCategoryList = FREE_CATEGORY_LIST;
    private final List<String> WhiteListappList = new ArrayList<String>();
    private final List<String> UserpkgName = new ArrayList<String>();
    private final List<String> SyspkgName = new ArrayList<String>();
    private final Hashtable<String, String> WebContent = new Hashtable<String, String>();
    private final Hashtable<String, Double> ApplicationRate = new Hashtable<String, Double>();
    private final Hashtable<String, Integer> ApplicationRateCount = new Hashtable<String, Integer>();
    private final Hashtable<String, Integer> ApplicationNumberDownoad = new Hashtable<String, Integer>();
    private final HashMap<String, String> Record = new HashMap<String, String>();
    private final List<String> RunningAppsList = new ArrayList<String>();
    private String OnRunningFrontgroundApplication;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //http://api.xposed.info/reference/de/robv/android/xposed/XSharedPreferences.
        //White List
        final XSharedPreferences sharedPreferences_whitelist = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.SHARED_WHITELIST_PKGS_PREFERENCES_FILE);
        //Setting
        final XSharedPreferences sharedPreferences_posit = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.SHARED_PREFERENCES_DEFAULT_POSITION);
        final XSharedPreferences sharedPreferences_customer = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.SHARED_PREDERENCES_DEFAULT_CUSTOMER);
        //Application reset
        final XSharedPreferences sharedPreferences_UserApplicationFile = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.USER_PACKET_NAME);
        final XSharedPreferences sharedPreferences_SystemApplicationFile = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.SYSTEM_PACKET_NAME);
        final XSharedPreferences sharedPreferences_WebContent = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.WEB_CONTENT);
        //Feedback
        final XSharedPreferences sharedPreferences_Feedback = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.FEEDBACK_COMFORTABLE);
        //Mode
        final XSharedPreferences sharedPreferences_ModeWork = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.MODE_WORK_SETUP);
        final XSharedPreferences sharedPreferences_ModeRest = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.MODE_REST_SETUP);
        //Background Service
        final XSharedPreferences RunningApps = new XSharedPreferences(BuildConfig.APPLICATION_ID, Common.BGDFGDRECORDKEY);

        sharedPreferences_whitelist.makeWorldReadable();
        sharedPreferences_posit.makeWorldReadable();
        sharedPreferences_customer.makeWorldReadable();
        sharedPreferences_UserApplicationFile.makeWorldReadable();
        sharedPreferences_SystemApplicationFile.makeWorldReadable();
        sharedPreferences_WebContent.makeWorldReadable();
        sharedPreferences_Feedback.makeWorldReadable();
        sharedPreferences_ModeWork.makeWorldReadable();
        sharedPreferences_ModeRest.makeWorldReadable();
        RunningApps.makeWorldReadable();

        WhiteListappList.clear();
        WhiteListappList.addAll(sharedPreferences_whitelist.getStringSet(Common.PREF_KEY_WHITELIST_APP_LIST, new HashSet<String>()));
        Collections.sort(WhiteListappList);

        //Category
        UserpkgName.clear();
        UserpkgName.addAll(sharedPreferences_UserApplicationFile.getStringSet(Common.USER_PACKET_NAME_KEY, new HashSet<String>()));
        Collections.sort(UserpkgName);
        SyspkgName.clear();
        SyspkgName.addAll(sharedPreferences_SystemApplicationFile.getStringSet(Common.SYSTEM_PACKET_NAME_KEY, new HashSet<String>()));
        Collections.sort(SyspkgName);

        final List<String> adapterWeb = new ArrayList<String>();
        adapterWeb.clear();
        adapterWeb.addAll(sharedPreferences_WebContent.getStringSet(Common.WEB_CONTENT_KEY, new HashSet<String>()));
        Collections.sort(adapterWeb);
        if (!(WebContent.size() == adapterWeb.size()))
            WebContent.clear();
        for (String Web : adapterWeb) {//TODO
            for (String User : UserpkgName) {
                if (Web.startsWith(User)) {
                    WebContent.put(User, Web.substring(User.length()));
                    continue;
                }
            }
            for (String System : SyspkgName) {
                    if (Web.startsWith(System)) {
                        WebContent.put(System, Web.substring(System.length()));
                        break;
                    }
            }
        }

        adapterWeb.clear();
        adapterWeb.addAll(sharedPreferences_WebContent.getStringSet(Common.WEB_CONTENT_RATE, new HashSet<String>()));
        Collections.sort(adapterWeb);
        if (!(ApplicationRate.size() == adapterWeb.size()))
            ApplicationRate.clear();
        for (String Web : adapterWeb) {
            Boolean next = true;
            for (String User : UserpkgName) {
                if (Web.startsWith(User)) {
                    String temp = Web.substring(User.length());
                    Double RateClassified = ClassTheRate(temp);
                    ApplicationRate.put(User, RateClassified);
                    next = false;
                    break;
                }
            }
            if (next) {
                for (String System : SyspkgName) {
                    if (Web.startsWith(System)) {
                        String temp = Web.substring(System.length());
                        Double RateClassified = ClassTheRate(temp);
                        ApplicationRate.put(System, RateClassified);
                        break;
                    }
                }
            }
        }

        adapterWeb.clear();
        adapterWeb.addAll(sharedPreferences_WebContent.getStringSet(Common.WEB_CONTENT_RATE_COUNT, new HashSet<String>()));
        Collections.sort(adapterWeb);
        if (!(ApplicationRateCount.size() == adapterWeb.size()))
             ApplicationRateCount.clear();
        for (String Web : adapterWeb) {
            Boolean next = true;
            for (String User : UserpkgName) {
                if (Web.startsWith(User)) {
                    String temp = Web.substring(User.length());
                    Integer Lenth = temp.length();
                    ApplicationRateCount.put(User, Lenth);
                    next = false;
                    break;
                }
            }
            if (next) {
                for (String System : SyspkgName) {
                    if (Web.startsWith(System)) {
                        String temp = Web.substring(System.length());
                        Integer Lenth = temp.length();
                        ApplicationRateCount.put(System, Lenth);
                        break;
                    }
                }
            }
        }
        adapterWeb.clear();
        adapterWeb.addAll(sharedPreferences_WebContent.getStringSet(Common.WEB_CONTENT_NUMDOWNLOAD, new HashSet<String>()));
        Collections.sort(adapterWeb);
        if (!(ApplicationNumberDownoad.size() == adapterWeb.size()))
            ApplicationNumberDownoad.clear();
        for (String Web : adapterWeb) {
            Boolean next = true;
            for (String User : UserpkgName) {
                if (Web.startsWith(User)) {
                    String temp = Web.substring(User.length());
                    Integer ho = temp.length();
                    ApplicationNumberDownoad.put(User, ho);
                    next = false;
                    break;
                }
            }
            if (next) {
                for (String System : SyspkgName) {
                    if (Web.startsWith(System)) {
                        String temp = Web.substring(System.length());
                        Integer ho = temp.length();
                        ApplicationNumberDownoad.put(System, ho);
                        break;
                    }
                }
            }
        }
        //Mode value
        double TimeModeCheck = -1;
        Calendar mcurrentTime = Calendar.getInstance();
        int Curr_Hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int Curr_Minute = mcurrentTime.get(Calendar.MINUTE);
        //Mode change
        final boolean WorkMode_ON = sharedPreferences_ModeWork.getBoolean(Common.MODE_WORK_SETUP_KEY, false);
        final int WorkMode_Start_Hour = sharedPreferences_ModeWork.getInt(Common.MODE_WORK_SETUP_STARTTIME_KEY_HOUR, -1);
        final int WorkMode_Start_Mintues = sharedPreferences_ModeWork.getInt(Common.MODE_WORK_SETUP_STARTTIME_KEY_MINUTES, -1);
        final int WorkMode_End_Hour = sharedPreferences_ModeWork.getInt(Common.MODE_WORK_SETUP_ENDTIME_KEY_HOUR, -1);
        final int WorkMode_End_Mintues = sharedPreferences_ModeWork.getInt(Common.MODE_WORK_SETUP_ENDTIME_KEY_MINUTES, -1);
        if (WorkMode_ON) {
            if (WorkMode_Start_Hour <= Curr_Hour) {
                if (WorkMode_Start_Mintues <= Curr_Minute) {
                    if (WorkMode_Start_Hour != -1)
                        TimeModeCheck = 0.9;
                }
            } else if (WorkMode_End_Hour <= Curr_Hour) {
                if (WorkMode_End_Mintues < Curr_Minute) {
                    TimeModeCheck = 1;
                    XposedBridge.log("Current " + Curr_Hour + ":" + Curr_Minute);
                }
            }
        } else
            TimeModeCheck = WorkMode_Start_Hour;

        Boolean RestMode_ON = sharedPreferences_ModeRest.getBoolean(Common.MODE_REST_SETUP_KEY, false);
        final int RestMode_Start_Hour = sharedPreferences_ModeRest.getInt(Common.MODE_REST_SETUP_STARTTIME_KEY_HOUR, -1);
        final int RestMode_Start_Mintues = sharedPreferences_ModeRest.getInt(Common.MODE_REST_SETUP_STARTTIME_KEY_MINUTES, -1);
        final int RestMode_End_Hour = sharedPreferences_ModeRest.getInt(Common.MODE_REST_SETUP_ENDTIME_KEY_HOUR, -1);
        final int RestMode_End_Mintues = sharedPreferences_ModeRest.getInt(Common.MODE_REST_SETUP_ENDTIME_KEY_MINUTES, -1);
        if (RestMode_ON) {
            if (RestMode_Start_Hour <= Curr_Hour) {
                if (RestMode_Start_Mintues <= Curr_Minute) {
                    TimeModeCheck = 1.1;
                }
            } else if (RestMode_End_Hour <= Curr_Hour) {
                if (RestMode_End_Mintues < Curr_Minute) {
                    TimeModeCheck = 1;
                }
            }
        }
        final boolean RunBgdFgd = RunningApps.getBoolean(Common.BGDFGDRECORDKEYUP, false);
        if (RunBgdFgd) {
            /*List<String> temp = new ArrayList<String>();
            temp.clear();
            temp.addAll(RunningApps.getStringSet(Common.BGDFGDAPPLICATION, new HashSet<String>()));
            Collections.sort(temp);
            XposedBridge.log("No such key");
            RunningAppsList.clear();
            for (String RunningApp : temp) {
                if(WebContent.containsKey(RunningApp)){
                    RunningAppsList.add(RunningApp);
                    XposedBridge.log("Running Application: " + RunningApp +"-");
                }
                else
                    XposedBridge.log("No such key");
            }*/
            RunningAppsList.clear();
            RunningAppsList.addAll(RunningApps.getStringSet(Common.BGDFGDRUNNINGAPPLICATION, new HashSet<String>()));
            Collections.sort(RunningAppsList);
            OnRunningFrontgroundApplication = RunningApps.getString(Common.CURRENTAPPLICATION, "No Application Running");//TODO
        }
        //https://www.google.com.hk/search?q=how+to+use+the+data+in+hashmap+android&spell=1&sa=X&ved=0ahUKEwjy3e_XuMHRAhWEn5QKHZqmCtcQvwUIGCgA&biw=1451&bih=660
        //http://blog.csdn.net/yzzst/article/details/47659479
        if (sdk > 18) {
            try {
                Random rand = new Random(sdk);
                int omg = sharedPreferences_posit.getInt(Common.SHARED_PREFERENCES_DEFAULT_POSITION, 0);
                // Latitudes range from -90 to 90.
                // Longitudes range from -180 to 180.
                int adapter = 1;
                if (omg == 0) {//Default

                    //XposedBridge.log("The User chose Default");
                } else if (omg == 1) {//Customer
                    adapter = sharedPreferences_customer.getInt(Common.SHARED_PREDERENCES_DEFAULT_CUSTOMER, 5);
                    XposedBridge.log("The User chose Customer and the value is " + adapter);
                    if(adapter >40) {
                        adapter %= 40;
                    }
                    if(adapter == 0) {
                        adapter = 5;
                    }

                    if (sdk >= 21)
                        adapter = ThreadLocalRandom.current().nextInt(1, adapter);
                    else
                        adapter = ((rand.nextInt(adapter))) + 1;
                } else if (omg >= 2) {//Low, Medium,Highest
                    //adapter = sharedPreferences.getInt(Common.SHARED_PREFERENCES_POSITION,0);
                    if (sdk >= 21)
                        adapter = ThreadLocalRandom.current().nextInt(1, (10 * omg)) + 1;
                    else
                        adapter = (rand.nextInt(10 * omg)) + 1;

                   // XposedBridge.log("The User chose Low, Medium, High.");
                } else
                    XposedBridge.log("The SharePreferences get wrong...");

                int FeedbackValue = 1;
                String Feedback_choice = sharedPreferences_Feedback.getString(Common.FEEDBACK_COMFORTABLE_KEY, " ");
                if (Objects.equals(Feedback_choice, "Strong")) {
                    FeedbackValue = adapter / 2;
                } else if (Objects.equals(Feedback_choice, "Week")) {
                    FeedbackValue = adapter * 2;
                } else if (Objects.equals(Feedback_choice, "Suitable")) {
                    FeedbackValue = adapter;
                } else if (Objects.equals(Feedback_choice, " ")) {
                    FeedbackValue = adapter;
                }
                final int range = FeedbackValue;
            /*
            Source file of android location api
            //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/location/java/android/location/Location.java
            //https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/location/java/android/location/LocationManager.java
            */
                if (omg != 0) {
                    if (sharedPreferences_whitelist.getBoolean(Common.PREF_KEY_WHITELIST_ALL, true) || WhiteListappList.contains(lpparam.packageName)) {
                        //https://android.googlesource.com/platform/frameworks/base/+/9637d474899d9725da8a41fdf92b9bd1a15d301e/core/java/android/provider/Settings.java
                        findAndHookMethod("android.provider.Settings.Secure", lpparam.classLoader, "getString",
                                ContentResolver.class, String.class, new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        String requested = (String) param.args[1];
                                        if (requested.equals(Settings.Secure.ALLOW_MOCK_LOCATION)) {
                                            param.setResult("0");
                                            XposedBridge.log("Loaded app: " + lpparam.packageName);
                                        }
                                    }
                                });
                    }
                    final double modeChange = TimeModeCheck;
                    findAndHookMethod(Common.SYSTEM_LOCATION, lpparam.classLoader, "getLatitude",
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    super.beforeHookedMethod(param);
                                    //XposedBridge.log("Here im Xpsoed Hooked");
                                }

                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    //super.afterHookedMethod(param);
                                    Random rand = new Random(range);
                                    String packageName = AndroidAppHelper.currentPackageName();
                                    String CurrpackageName = lpparam.packageName;
                                    double popular = SearchInFromWebContent(CurrpackageName);
                                    //Original value + random value
                                    double ha = (rand.nextDouble() % (MaxLat));
                                    double he = (rand.nextDouble() % (range*popular)) % 0.1 * modeChange / 10;
                                    double RanLat =
                                            BigDecimal.valueOf(ha % he)
                                                    .setScale(5, RoundingMode.HALF_UP)
                                                    .doubleValue();
                                    if(Objects.equals(packageName, CurrpackageName)) {
                                        RanLat = RunningApplicationPlusNoise(RanLat, RunningAppsList.contains(CurrpackageName));
                                        RanLat = FroundApplication(RanLat, OnRunningFrontgroundApplication.startsWith(CurrpackageName));
                                    }
                                    else{
                                        RanLat = RunningApplicationPlusNoise(RanLat, RunningAppsList.contains(packageName));
                                        RanLat = FroundApplication(RanLat, OnRunningFrontgroundApplication.startsWith(packageName));
                                    }
                                    String ho123 = Record.get(packageName);
                                    String ha123 = Record.get(CurrpackageName);
                                    try {
                                        double ori = (double) param.getResult();//get the original result
                                        //TODO escape
                                        String Category_1 = WebContent.get(packageName);
                                        String Category_2 = WebContent.get(CurrpackageName);
                                        //Check the package in free list -> created by admin
                                        if (Arrays.asList(FreeCategoryList).contains(Category_1)) {
                                            param.setResult(ori);
                                            XposedBridge.log(packageName + " needs the accuracy location cause the category is " + Category_1);
                                            if (Objects.equals(ho123, ha123))
                                                Record.put(ho123, "Original");
                                        } else if (Arrays.asList(FreeCategoryList).contains(Category_2)) {
                                            param.setResult(ori);
                                            XposedBridge.log(packageName + " needs the accuracy location cause the category is " + Category_2);
                                        } else if (Arrays.asList(FreePackageList).contains(packageName) || Arrays.asList(FreePackageList).contains(CurrpackageName)) {
                                            double ra =
                                                        BigDecimal.valueOf(rand.nextDouble() % 0.1)
                                                                .setScale(5, RoundingMode.HALF_UP)
                                                                .doubleValue();
                                                ra = MakeItNegOrPost(ra, range) / 10000;
                                                ra += ori;
                                                param.setResult(ra);
                                                XposedBridge.log(CurrpackageName + " needs the seems accuracy location - " + ra);
                                        }
                                        //within white list
                                        else if ((WhiteListappList.contains(packageName)) || (WhiteListappList.contains(CurrpackageName))) {
                                            param.setResult(ori);
                                            XposedBridge.log(packageName + " needs the accuracy location cause it listed in whitelist");
                                        }
                                        else if (Objects.equals(ha123, "Original")) {
                                            param.setResult(ori);
                                        } else {
                                            //Match apart of
                                            for (String List_keyword : FreeKeywordList) {
                                                if (packageName.startsWith(List_keyword)) {
                                                    double result = ori + MakeItNegOrPost(RanLat, range);
                                                    param.setResult(result);
                                                    XposedBridge.log(packageName + " get the Latitude " + result);
                                                } else if (RunningAppsList.contains(CurrpackageName) || RunningAppsList.contains(packageName)) {
                                                    double result = ori + (MakeItNegOrPost(RanLat, range) * 1.00000001);
                                                    if ( OnRunningFrontgroundApplication.startsWith(packageName)
                                                            ||  OnRunningFrontgroundApplication.startsWith(CurrpackageName)) {
                                                        result *= 1.00000005;
                                                    }
                                                    param.setResult(result);
                                                    XposedBridge.log("The running application " + packageName + " get the Latitude " + result);
                                                } else {
                                                    double result = ori + (MakeItNegOrPost(RanLat, range) * 1.0000001);
                                                    param.setResult(result);
                                                    XposedBridge.log(packageName + " get the Latitude " + result);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        XposedBridge.log("Problem 1 at " + e);
                                    }
                                }
                            });
                    findAndHookMethod(Common.SYSTEM_LOCATION, lpparam.classLoader, "getLongitude", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            //XposedBridge.log("Here im Xpsoed Hooked");
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            //super.afterHookedMethod(param); Math.floor
                            Random rand = new Random(range);
                            String packageName = AndroidAppHelper.currentPackageName();
                            String CurrpackageName = lpparam.packageName;
                            //Original value + random value
                            double ha = (rand.nextDouble() % (MaxLong));
                            double popular = SearchInFromWebContent(CurrpackageName);
                            double he = (rand.nextDouble() % (range*popular)) % 0.1 * modeChange /10;
                            double RanLong =
                                    BigDecimal.valueOf(ha % he)
                                            .setScale(5, RoundingMode.HALF_UP)
                                            .doubleValue();
                            if(Objects.equals(packageName, CurrpackageName)) {
                                RanLong = RunningApplicationPlusNoise(RanLong, RunningAppsList.contains(CurrpackageName));
                                RanLong = FroundApplication(RanLong, OnRunningFrontgroundApplication.startsWith(CurrpackageName));
                            }
                            else{
                                RanLong = RunningApplicationPlusNoise(RanLong, RunningAppsList.contains(packageName));
                                RanLong = FroundApplication(RanLong, OnRunningFrontgroundApplication.startsWith(packageName));
                            }
                            List<String> appList = new ArrayList<String>();
                            appList.addAll(sharedPreferences_whitelist.getStringSet(Common.PREF_KEY_WHITELIST_APP_LIST, new HashSet<String>()));
                            Collections.sort(appList);
                            try {
                                double ori = (double) param.getResult();//get the original result
                                //TODO escape
                                String Category_1 = WebContent.get(packageName);
                                String Category_2 = WebContent.get(CurrpackageName);
                                //Check the package in free list -> created by admin
                                if (Arrays.asList(FreeCategoryList).contains(Category_1)) {
                                    param.setResult(ori);
                                    XposedBridge.log(packageName + " needs the accuracy location cause the category is " + Category_1);
                                } else if (Arrays.asList(FreeCategoryList).contains(Category_2)) {
                                    param.setResult(ori);
                                    XposedBridge.log(packageName + " needs the accuracy location cause the category is " + Category_2);
                                } else if (Arrays.asList(FreePackageList).contains(packageName) || Arrays.asList(FreePackageList).contains(CurrpackageName)) {
                                        double ra = BigDecimal.valueOf(rand.nextDouble() % 0.001)
                                                .setScale(5, RoundingMode.HALF_UP)
                                                .doubleValue();
                                        ra = MakeItNegOrPost(ra, range) /10000 ;
                                        ra += ori;
                                        param.setResult(ra);
                                        XposedBridge.log(CurrpackageName + " needs the seems accuracy location - " + ra);
                                }
                                else if ((WhiteListappList.contains(packageName)) || (WhiteListappList.contains(CurrpackageName))) {
                                    param.setResult(ori);
                                    XposedBridge.log(packageName + " needs the accuracy location cause it listed in whitelist");
                                }
                                else {
                                    //Match apart of
                                    for (String List_keyword : FreeKeywordList) {
                                        if (packageName.startsWith(List_keyword)) {
                                            double result = ori + MakeItNegOrPost(RanLong, range);
                                            param.setResult(result);
                                            XposedBridge.log(packageName + " get the Longitude " + result);
                                        } else if (RunningAppsList.contains(CurrpackageName) || RunningAppsList.contains(packageName)) {
                                            double result = ori + (MakeItNegOrPost(RanLong, range) * 1.00000001);
                                            if ( OnRunningFrontgroundApplication.startsWith(packageName)
                                                    ||  OnRunningFrontgroundApplication.startsWith(CurrpackageName)) {
                                                result *= 1.00000005;
                                            }
                                            param.setResult(result);
                                            XposedBridge.log("The running application " + packageName + " get the Latitude " + result);
                                        } else {
                                            double result = ori + (MakeItNegOrPost(RanLong, range) * 1.0000001);
                                            param.setResult(result);
                                            XposedBridge.log(packageName + " get the Longitude " + result);
                                        }
                                    }

                                }
                            } catch (Exception e) {
                                XposedBridge.log("Problem 1 at " + e);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                XposedBridge.log("Wrong here");
            }
        }
    }

    private double MakeItNegOrPost(double haha, int range) {
        double change = haha;
        Random rand = new Random(sdk);
        if (rand.nextBoolean())
            change *= (-1);
        return change;
    }

    private Double ClassTheRate(String val) {
        Double hehe = 0.0;
        if (val.startsWith("0")) {
            if (!val.endsWith("0")) {
                hehe = 0.5;
            }
        } else if (val.startsWith("1")) {
            if (!val.endsWith("1")) {
                hehe = 1.5;
            } else {
                hehe = 1.0;
            }
        } else if (val.startsWith("2")) {
            if (!val.endsWith("2")) {
                hehe = 2.5;
            } else {
                hehe = 2.0;
            }
        } else if (val.startsWith("3")) {
            if (!val.endsWith("3")) {
                hehe = 3.5;
            } else {
                hehe = 3.0;
            }
        } else if (val.startsWith("4")) {
            if (!val.endsWith("4")) {
                hehe = 4.5;
            } else {
                hehe = 4.0;
            }
        } else {
            hehe = 5.0;
        }
        return hehe;
    }

    private double SearchInFromWebContent(String CurrpackageName){
        Double rate = ApplicationRate.get(CurrpackageName);
        Integer ratecount = ApplicationRateCount.get(CurrpackageName);
        Integer numofDownloader_Length = ApplicationNumberDownoad.get(CurrpackageName);

        if(rate == (null))
            return 1.0;
        if(ratecount == (null))
            return 1.0;
        if(numofDownloader_Length == (null))
            return 1.0;
        double hehe = 1.00005;
        if(numofDownloader_Length >= 20){ // more or equal to 1 million
            return 1.0;
        }
        else{
            if(numofDownloader_Length >= 15 ){ //more than 10 thousand
                if(rate > 2.5){
                    if(ratecount > 5) { //at least 10 thousand
                        return 1.00001;
                    }
                } else if(ratecount > 5){ //more or equal to 1 thousand
                    return 1.00002;
                }
                else{
                   return 1.00003;
                }
            }
            else if(numofDownloader_Length > 12){ //more or equal to 1 thousand
                if(rate > 2.5){
                    if(ratecount > 3) { //at least 1 hundred
                        return 1.00003;
                    }
                    else{
                        return 1.00004;
                    }
                }
                else{
                    return 1.00004;
                }
            }
            else{
                return 1.00005;
            }
        }
        return hehe;
    }

    private double RunningApplicationPlusNoise (double noise, boolean runningORNot){
        if(runningORNot)
            return noise*0.9999999975;
        return noise;
    }

    private double FroundApplication (double noise, boolean runningORNot){
        if(runningORNot)
             return  noise*0.9999995;
        return noise;
    }
}
