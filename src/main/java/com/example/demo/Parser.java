package com.example.demo;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Controller
public class Parser {

    private final OkHttpClient httpClient = new OkHttpClient();
    List<String> languageURLs = Arrays.asList(
            "https://www.investing.com/equities/at-t-commentary",
            //"https://ru.investing.com/equities/at-t-commentary",
            //"https://uk.investing.com/equities/at-t-commentary",
            "https://ca.investing.com/equities/at-t-commentary"
            //"https://de.investing.com/equities/at-t-commentary",
            //"https://fr.investing.com/equities/at-t-commentary",
            //"https://cn.investing.com/equities/at-t-commentary"
            //"https://in.investing.com/equities/at-t-commentary"
    );

    @RequestMapping(value = "/api/foo.csv")
    public String fooAsCSV(HttpServletResponse response) throws IOException {
        FileReader fr = new FileReader("c:/temp/testDB.txt");
        Scanner scan = new Scanner(fr);
        StringBuilder output = new StringBuilder();
        while (scan.hasNextLine()) {
            output.append(scan.nextLine());
        }

        fr.close();
        response.setContentType("text/html; charset=utf-8");
        response.getWriter().print(output.toString());
        return output.toString();
    }

    @RequestMapping("/getCsvData")
    private void sendPost() throws Exception {
        ArrayList<StockPrice> listOfPrice = getListOfPrice("244", "01/01/2016", "12/12/2019");
        // Тут цикл сделать по currId от 1 до 600000 с задержкой случайной 5-10 секунд
        ArrayList<CommentData> listOfComments = new ArrayList<CommentData>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mmaaa", Locale.ENGLISH);
        SimpleDateFormat simpleDateFormatRu = new SimpleDateFormat("dd/mm/yyyy");
        SimpleDateFormat simpleDateFormatRu1 = new SimpleDateFormat("dd/mm/yyyy");

        Date fromDate = simpleDateFormatRu.parse("06/11/2019");
        Date ToDate = simpleDateFormatRu.parse("06/12/2019");

        for (String url : languageURLs) {
            // Тут цикл сделать по спаршеному Url
            listOfComments.addAll(getAllCommentsFromInvestingCom(url));
        }

        ArrayList<OutPutDate> listofOutPutDate = new ArrayList<>();
        //Получил список цен на акцию + все комменты с датами, ниже марджу их
        listOfComments.forEach((commentTmp) -> {
            try {
                OutPutDate outPutDateTMP = new OutPutDate();
                outPutDateTMP.date = changeData(commentTmp.data);
                outPutDateTMP.comment = commentTmp.Comment;

                listOfPrice.forEach((parsePrice) -> {
                    if (parsePrice.data.equals(outPutDateTMP.date)) {
                        outPutDateTMP.priceStart = String.valueOf(parsePrice.openPrice);
                        outPutDateTMP.priceEnd = String.valueOf(parsePrice.price);
                    }
                });

                listofOutPutDate.add(outPutDateTMP);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        StringBuilder outputComment = new StringBuilder();
        outputComment.append("comment\n");

        for (OutPutDate outPutDate : listofOutPutDate) {
            outputComment.append("\"").append(outPutDate.comment).append("\"\n");
        }

        FileWriter fileWriter = new FileWriter("c:/temp/commentDB.txt");
        fileWriter.append(outputComment);
        fileWriter.flush();
        fileWriter.close();

        StringBuilder outputPrice = new StringBuilder();
        outputPrice.append("PriceChange\n");

        for (OutPutDate outPutDate : listofOutPutDate) {
            if (outPutDate.priceEnd == null || outPutDate.priceStart == null)
                outputPrice.append("\"1\"\n");
            else
                outputPrice.append("\"").append(Float.parseFloat(outPutDate.priceEnd) / Float.parseFloat(outPutDate.priceStart)).append("\"\n");
        }

        FileWriter fileWriter1 = new FileWriter("c:/temp/priceChangeDB.txt");
        fileWriter1.append(outputPrice);
        fileWriter1.flush();
        fileWriter1.close();
    }


    private String changeData(String dat) throws ParseException {
        final String RU_FORMAT = "dd.MM.yyyy";
        dat = dat.substring(0, dat.length() - 3);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mmaaa", Locale.ENGLISH);
        Date dateTMP = simpleDateFormat.parse(dat);
        return dateFormater(dat, RU_FORMAT, "MMM dd, yyyy HH:mmaaa");
    }

    public static String dateFormater(String dateFromJSON, String expectedFormat, String oldFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(oldFormat, Locale.ENGLISH);
        Date date = null;
        String convertedDate = null;
        try {
            date = dateFormat.parse(dateFromJSON);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(expectedFormat);
            convertedDate = simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    private ArrayList<StockPrice> getListOfPrice(String currId, String DataStart, String DataEnd) throws Exception {
        RequestBody formBody = new FormBody.Builder()
                .add("curr_id", currId)// AT&T 244
                .add("smlID", "1233408")
                .add("header", "Прошлые данные - AT&T")
                .add("st_date", DataStart)
                .add("end_date", DataEnd)
                .add("interval_sec", "Daily")
                .add("sort_col", "date")
                .add("sort_ord", "DESC")
                .add("action", "historical_data")
                .build();

        Request request = new Request.Builder()
                .url("https://ru.investing.com/instruments/HistoricalDataAjax")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Host", "ru.investing.com")
                .addHeader("Connection", "keep-alive")
                .addHeader("Accept", "text/plain, */*; q=0.01")
                .addHeader("Origin", "https://ru.investing.com")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Mobile Safari/537.36")
                .post(formBody)
                .build();
        ArrayList<StockPrice> stockPrices = new ArrayList<>();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            Document doc = Jsoup.parse(response.body().string());
            Elements dayslines = doc.select("tr");
            dayslines.remove(dayslines.last());
            for (Element days : dayslines) {
                Elements daysPriceParced = days.select("td");
                if (daysPriceParced.size() > 0) {
                    StockPrice newStockPriceTMP = new StockPrice();
                    newStockPriceTMP.data = daysPriceParced.get(0).text();
                    newStockPriceTMP.price = Double.parseDouble(daysPriceParced.get(1).text().replace(',', '.'));
                    newStockPriceTMP.openPrice = Double.parseDouble(daysPriceParced.get(2).text().replace(',', '.'));
                    newStockPriceTMP.maxPrice = Double.parseDouble(daysPriceParced.get(3).text().replace(',', '.'));
                    newStockPriceTMP.minPrice = Double.parseDouble(daysPriceParced.get(4).text().replace(',', '.'));
                    newStockPriceTMP.vol = daysPriceParced.get(5).text();
                    newStockPriceTMP.changePrice = daysPriceParced.get(6).text();//Лень мапить и времени мало

                    stockPrices.add(newStockPriceTMP);
                }
            }
        }
        return stockPrices;
    }
    // Цена акции
    // Комментарии из investing.com

    private ArrayList<CommentData> getCommentFromPage(Document htmlDocument) {
        ArrayList<CommentData> allComments = new ArrayList<CommentData>();
        Elements newsHeadlines = htmlDocument.select(".mainComment");
        //TODO Переписать нормально
        for (Element commentContent : newsHeadlines) {
            CommentData commentData = new CommentData();
            commentData.Comment = commentContent.select(".js-text").get(0).text();
            commentData.data = commentContent.select(".js-date").get(0).text();
            allComments.add(commentData);
        }
        newsHeadlines = htmlDocument.select(".commentInnerWrapper");//зарефачить
        for (Element commentContent : newsHeadlines) {
            CommentData commentData = new CommentData();
            commentData.Comment = commentContent.select(".js-text").get(0).text();
            commentData.data = commentContent.select(".js-date").get(0).text();
            allComments.add(commentData);
        }
        return allComments;
    }

    private ArrayList<CommentData> getAllCommentsFromInvestingCom(String URL) throws Exception {
        ArrayList<CommentData> allComments = new ArrayList<CommentData>();
        Random rand = new Random();
        String page = "1";
        Document doc = Jsoup.connect(URL).get();
        String urlTmp = URL;

        int iter = 0;

        while (doc.select(".sideDiv").size() > 1 && !doc.select(".sideDiv").get(1).text().equals("")) {
            allComments.addAll(getCommentFromPage(doc));
            page = Integer.toString(Integer.parseInt(page) + 1);
            URL = urlTmp + "/" + page;
            doc = Jsoup.connect(URL).get();
            getCommentFromPage(doc);
            Thread.sleep(rand.nextInt(4000) + 1000);
        }

        return allComments;
    }


}
