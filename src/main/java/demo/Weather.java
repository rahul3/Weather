package demo;

import com.google.gson.Gson;
import openweather.OpenWeatherService;
import openweather.OpenWeatherMapException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.nlpcraft.utils.keycdn.GeoManager;
import org.apache.nlpcraft.utils.keycdn.beans.GeoDataBean;
import org.apache.nlpcraft.model.*;
import java.time.Instant;
import java.util.*;
import static java.time.temporal.ChronoUnit.DAYS;

public class Weather extends NCModelFileAdapter {
    // Please register your own account at https://openweathermap.org/api and
    // replace this demo token with your own.
    // We are using the One Call API (https://openweathermap.org/api/one-call-api) in this example
    private final OpenWeatherService openWeather = new OpenWeatherService("<enter-your-key-here>", 5, 7);

    private final GeoManager geoMrg = new GeoManager();
    private static final int DAYS_SHIFT = 5;
    private static final Gson GSON = new Gson();
    private static final Set<String> LOCAL_WORDS = new HashSet<>(Arrays.asList("my", "local", "hometown"));

    private Pair<Double, Double> prepGeo(NCIntentMatch ctx, Optional<NCToken> geoTokOpt) throws NCRejection {
        if (geoTokOpt.isPresent()) {
            NCToken geoTok = geoTokOpt.get();

            Map<String, Object> cityMeta = geoTok.meta("nlpcraft:city:citymeta");

            Double lat = (Double)cityMeta.get("latitude");
            Double lon = (Double)cityMeta.get("longitude");

            if (lat == null || lon == null) {
                String city = geoTok.meta("nlpcraft:city:city");

                throw new NCRejection(String.format("Latitude and longitude not found for: %s", city));
            }

            return Pair.of(lat, lon);
        }

        Optional<GeoDataBean> geoOpt = geoMrg.get(ctx.getContext().getRequest());

        if (!geoOpt.isPresent())
            throw new NCRejection("City cannot be determined.");

        // Manually process request for local weather. We need to separate between 'local Moscow weather'
        // and 'local weather' which are different. Basically, if there is word 'local/my/hometown' in the user
        // input and there is no city in the current sentence - this is a request for the weather at user's
        // current location, i.e. we should implicitly assume user's location and clear conversion context.
        // In all other cases - we take location from either current sentence or conversation STM.

        // NOTE: we don't do this separation on intent level as it is easier to do it here instead of
        // creating more intents with almost identical callbacks.

        @SuppressWarnings("SuspiciousMethodCalls")
        boolean hasLocalWord =
                ctx.getVariant().stream().anyMatch(t -> LOCAL_WORDS.contains(t.meta("nlpcraft:nlp:origtext")));

        if (hasLocalWord)
            // Because we implicitly assume user's current city at this point we need to clear
            // 'nlpcraft:city' tokens from conversation since they would no longer be valid.
            ctx.getContext().getConversation().clearStm(t -> t.getId().equals("nlpcraft:city"));

        // Try current user location.
        GeoDataBean geo = geoOpt.get();

        return Pair.of(geo.getLatitude(), geo.getLongitude());
    }

    @NCIntent(
            "intent=req " +
                    "  term~{tok_id() == 'wt:phen'}* " + // Zero or more weather phenomenon.
                    "  term(ind)~{has(tok_groups(), 'indicator')}* " + // Optional indicator words (zero or more).
                    "  term(city)~{tok_id() == 'nlpcraft:city'}? " + // Optional city.
                    "  term(date)~{tok_id() == 'nlpcraft:date'}?" // Optional date (overrides indicator words).
    )
    @NCIntentSample({
            "What's the local weather forecast?",
            "What's the weather in Moscow?",
            "What is the weather like outside?",
            "How's the weather?",
            "What's the weather forecast for the rest of the week?",
            "What's the weather forecast this week?",
            "What's the weather out there?",
            "Is it cold outside?",
            "Is it hot outside?",
            "Will it rain today?",
            "When it will rain in Delhi?",
            "Is there any possibility of rain in Delhi?",
            "Is it raining now?",
            "Is there any chance of rain today?",
            "Was it raining in Beirut three days ago?",
            "How about yesterday?"
    })
    public NCResult onMatch(
            NCIntentMatch ctx,
            @NCIntentTerm("ind") List<NCToken> indToksOpt,
            @NCIntentTerm("city") Optional<NCToken> cityTokOpt,
            @NCIntentTerm("date") Optional<NCToken> dateTokOpt
    ) {
        // Reject if intent match is not exact (at least one "dangling" token remain).
        if (ctx.isAmbiguous())
            throw new NCRejection("Please clarify your request.");

        try {
            Instant now = Instant.now();

            Instant from = now;
            Instant to = now;

            if (indToksOpt.stream().anyMatch(tok -> tok.getId().equals("wt:hist")))
                from = from.minus(DAYS_SHIFT, DAYS);
            else if (indToksOpt.stream().anyMatch(tok -> tok.getId().equals("wt:fcast")))
                to = from.plus(DAYS_SHIFT, DAYS);

            if (dateTokOpt.isPresent()) { // Date token overrides any indicators.
                NCToken dateTok = dateTokOpt.get();

                from = Instant.ofEpochMilli(dateTok.meta("nlpcraft:date:from"));
                to = Instant.ofEpochMilli(dateTok.meta("nlpcraft:date:to"));

                System.out.println("LOG: from = " + from + " and TO: " + to);
            }

            Pair<Double, Double> latLon = prepGeo(ctx, cityTokOpt); // Handles optional city too.

            double lat = latLon.getLeft();
            double lon = latLon.getRight();

            return NCResult.json(GSON.toJson(from == to ? openWeather.getCurrent(lat, lon) :
                    openWeather.getTimeMachine(lat, lon, from, to)));
        }
        catch (OpenWeatherMapException e) {
            throw new NCRejection(e.getLocalizedMessage());
        }
        catch (NCRejection e) {
            throw e;
        }
        catch (Exception e) {
            throw new NCRejection("Weather provider error.", e);
        }
    }

    public Weather() {
        // Load model from external JSON file on classpath.
        super("weather.json");
    }

    @Override
    public void onDiscard() {
        openWeather.stop();
    }
}