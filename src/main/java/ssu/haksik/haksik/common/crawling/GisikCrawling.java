package ssu.haksik.haksik.common.crawling;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ssu.haksik.haksik.gisik.Gisik;
import ssu.haksik.haksik.gisik.GisikRepository;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class GisikCrawling {
    private final GisikRepository gisikRepository;

    private Elements crawling() throws IOException {
        String URL = "https://ssudorm.ssu.ac.kr:444/SShostel/mall_main.php?viewform=B0001_foodboard_list&board_no=1";
        Document document = Jsoup.connect(URL).get();
        Elements elements = document.getElementsByAttributeValue("class", "boxstyle02");
        Element element = elements.get(0);
        Elements tr = element.select("tr");
        return tr;
    }

    @Transactional
    @Scheduled(cron = "0 0 1 * * 1")
    void saveGisik() throws IOException {
        Elements tr= this.crawling();
        for (int day=1;day<8;day++) {
            Elements td = tr.get(day).getElementsByTag("td");
            for (int time =0; time<3; time++) {
                Element timeElement = td.get(time);
                String foods = timeElement.html().replace("<br>", "\n").replace(" ", "").replace("&amp;", "&");
                String result = "<오늘의 메뉴>\n\n".concat(foods);
                Gisik gisikExist = this.gisikRepository.findByEatingTimeAndDay(time, day);

                if (gisikExist == null) {
                    Gisik gisik = new Gisik(result, day, time);
                    this.gisikRepository.save(gisik);
                } else {
                    this.gisikRepository.updateGisikFoods(result, day, time);
                }
            }
        }

    }

    @Transactional(readOnly = true)
    public String getGisik(int eatingTime){
        DayOfWeek dayOfWeek = LocalDateTime.now().getDayOfWeek();
        int day = dayOfWeek.getValue();

        Gisik gisik =this.gisikRepository.findByEatingTimeAndDay(eatingTime, day);
        return gisik.getFoods();
    }


}