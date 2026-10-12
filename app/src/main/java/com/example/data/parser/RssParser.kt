package com.example.data.parser

import android.util.Xml
import com.example.data.model.ArticleEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader
import java.util.regex.Pattern

object RssParser {

    private val IMG_REGEX = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val HTML_TAG_REGEX = Pattern.compile("<[^>]*>")

    fun parse(inputStream: InputStream): List<ArticleEntity> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, "UTF-8")
        return readFeed(parser)
    }

    fun parseString(xmlString: String): List<ArticleEntity> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xmlString))
        return readFeed(parser)
    }

    private fun readFeed(parser: XmlPullParser): List<ArticleEntity> {
        val articles = mutableListOf<ArticleEntity>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            if (eventType == XmlPullParser.START_TAG && name.equals("item", ignoreCase = true)) {
                readItem(parser)?.let { articles.add(it) }
            }
            eventType = parser.next()
        }
        return articles
    }

    private fun readItem(parser: XmlPullParser): ArticleEntity? {
        var title = ""
        var link = ""
        var guid = ""
        var pubDate = ""
        var rawDescription = ""
        var imageUrl: String? = null
        val categories = mutableListOf<String>()
        var authorName: String? = null
        var authorTitle: String? = null
        var authorDepartment: String? = null

        var depth = 1
        while (depth != 0) {
            val eventType = parser.next()
            if (eventType == XmlPullParser.END_DOCUMENT) break

            val tag = parser.name ?: ""

            if (eventType == XmlPullParser.START_TAG) {
                depth++
                when {
                    tag.equals("title", ignoreCase = true) && depth == 2 -> {
                        title = readText(parser).trim()
                        depth--
                    }
                    tag.equals("link", ignoreCase = true) && depth == 2 -> {
                        link = readText(parser).trim()
                        depth--
                    }
                    tag.equals("guid", ignoreCase = true) && depth == 2 -> {
                        guid = readText(parser).trim()
                        depth--
                    }
                    tag.equals("pubDate", ignoreCase = true) && depth == 2 -> {
                        pubDate = readText(parser).trim()
                        depth--
                    }
                    tag.equals("description", ignoreCase = true) && depth == 2 -> {
                        rawDescription = readText(parser).trim()
                        depth--
                    }
                    tag.equals("content", ignoreCase = true) || tag.endsWith(":content", ignoreCase = true) -> {
                        val urlAttr = parser.getAttributeValue(null, "url")
                        if (!urlAttr.isNullOrBlank()) {
                            imageUrl = urlAttr
                        }
                    }
                    tag.equals("category", ignoreCase = true) -> {
                        val cat = readText(parser).trim()
                        if (cat.isNotEmpty()) {
                            categories.add(cat)
                        }
                        depth--
                    }
                    tag.equals("author", ignoreCase = true) || tag.endsWith(":author", ignoreCase = true) -> {
                        val authorData = readAuthor(parser)
                        depth--
                        authorName = authorData.first
                        authorTitle = authorData.second
                        authorDepartment = authorData.third
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                depth--
            }
        }

        if (imageUrl.isNullOrBlank() && rawDescription.isNotEmpty()) {
            val matcher = IMG_REGEX.matcher(rawDescription)
            if (matcher.find()) {
                imageUrl = matcher.group(1)
            }
        }

        val cleanDescription = HTML_TAG_REGEX.matcher(rawDescription).replaceAll("").trim()

        if (title.isBlank() && guid.isBlank()) return null
        if (guid.isBlank()) guid = link.ifBlank { title }

        return ArticleEntity(
            guid = guid,
            title = title,
            link = link,
            imageUrl = imageUrl,
            description = cleanDescription.ifBlank { title },
            pubDate = pubDate,
            categoriesCsv = categories.joinToString("|"),
            authorName = authorName,
            authorTitle = authorTitle,
            authorDepartment = authorDepartment,
            isBookmarked = false,
            cachedAt = System.currentTimeMillis()
        )
    }

    private fun readAuthor(parser: XmlPullParser): Triple<String?, String?, String?> {
        var name: String? = null
        var title: String? = null
        var dept: String? = null

        var depth = 1
        while (depth != 0) {
            val eventType = parser.next()
            if (eventType == XmlPullParser.END_DOCUMENT) break

            val tag = parser.name ?: ""
            if (eventType == XmlPullParser.START_TAG) {
                depth++
                when {
                    tag.equals("name", ignoreCase = true) -> {
                        name = readText(parser).trim()
                        depth--
                    }
                    tag.equals("title", ignoreCase = true) -> {
                        title = readText(parser).trim()
                        depth--
                    }
                    tag.equals("department", ignoreCase = true) -> {
                        dept = readText(parser).trim()
                        depth--
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                depth--
            }
        }
        return Triple(name, title, dept)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    val SEED_RSS_XML = """
<rss xmlns:atom="http://www.w3.org/2005/Atom" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
<channel>
<title>News from Google</title>
<link>https://blog.google/</link>
<description>News from Google</description>
<atom:link href="https://blog.google/rss/" rel="self"/>
<language>en-us</language>
<lastBuildDate>Tue, 18 Aug 2026 19:00:00 +0000</lastBuildDate>
<image>
<url>https://blog.google/static/blogv2/images/google.png</url>
<title>News from Google</title>
<link>https://blog.google/</link>
</image>
<item>
<title>Tap into the power of Gemini in Chrome on Android.</title>
<link>https://blog.google/products-and-platforms/products/chrome/gemini-in-chrome-android-auto-browse/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Chrome_Chewy_Bowl_Static_1.2.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Chrome_Chewy_Bowl_Static_1.2.max-600x600.format-webp.webp">Gemini in Chrome is now available to all Android users in the U.S.</description>
<pubDate>Tue, 18 Aug 2026 19:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/products/chrome/gemini-in-chrome-android-auto-browse/</guid>
<category>Android</category>
<category>Chrome</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Charmaine Dsilva</name>
<title>Director</title>
<department>Product Management, Chrome</department>
<company/>
</author>
</item>
<item>
<title>Ask a Scientist: How can researchers use AI to predict a flood?</title>
<link>https://blog.google/innovation-and-ai/technology/research/flood-prediction-ai/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Flood_Thumbnail_001.max-600x600.format-webp.webp" width="540"/>
<media:description>On the left: A blue background with black text saying 'Google' and 'Ask a scientist about flood forecasting.' On the right: Two Googlers in a virtual interview about flood forecasting</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Flood_Thumbnail_001.max-600x600.format-webp.webp">A Google researcher explains how AI technology like Flood Hub and Groundsource can predict floods and help people around the world.</description>
<pubDate>Tue, 18 Aug 2026 16:00:00 +0000</pubDate>
<guid>https://blog.google/innovation-and-ai/technology/research/flood-prediction-ai/</guid>
<category>Google Research</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Hannah Hunt</name>
<title>Contributor</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Operation Blue Skies: Reducing aviation climate impact with AI</title>
<link>https://blog.google/innovation-and-ai/models-and-research/google-research/blue-skies/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Google_blog_image.max-600x600.format-webp.webp" width="540"/>
<media:description>A high-altitude view of a commercial airplane flying above a layer of clouds, with a distinct white contrail trailing behind it.</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Google_blog_image.max-600x600.format-webp.webp">Google is partnering with the UK on Operation Blue Skies, an airspace-scale, AI-powered contrail avoidance trial.</description>
<pubDate>Tue, 18 Aug 2026 09:00:00 +0000</pubDate>
<guid>https://blog.google/innovation-and-ai/models-and-research/google-research/blue-skies/</guid>
<category>UK</category>
<category>Google Research</category>
<category>Sustainability</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Paul Hodgson</name>
<title>Technical Program Manager</title>
<department/>
<company/>
</author>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Chaim Langermann</name>
<title>Senior Program Manager</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Get closer to the game with Gemini and Pixel</title>
<link>https://blog.google/products-and-platforms/products/gemini/google-gemini-pixel-football-club-partnerships/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/football_multi-club_hero_nphU3P.max-600x600.format-webp.webp" width="540"/>
<media:description>Low-angle view of a soccer player kicking a ball mid-air against a bright blue sky, with grass flying from their cleats.</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/football_multi-club_hero_nphU3P.max-600x600.format-webp.webp">Google Gemini and Pixel partner with five global football clubs to elevate the fan matchday experience through AI and Smartphone Technology.</description>
<pubDate>Mon, 17 Aug 2026 08:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/products/gemini/google-gemini-pixel-football-club-partnerships/</guid>
<category>Gemini</category>
<category>AI</category>
<category>Google in Europe</category>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Eileen Mannion</name>
<title>VP, Marketing UKI and EMEA Devices and Services</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Introducing Gemini 3.7 Flash</title>
<link>https://blog.google/innovation-and-ai/models-and-research/gemini-models/introducing-gemini-3-7-flash/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/gemini-3-7-flash.max-600x600.format-webp.webp" width="540"/>
<media:description>Spark icon next to the text "Gemini 3.7 Flash", all on a light blue backgorund</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/gemini-3-7-flash.max-600x600.format-webp.webp">Gemini 3.7 Flash is our most intelligent workhorse model yet for coding and agents.</description>
<pubDate>Thu, 13 Aug 2026 17:00:00 +0000</pubDate>
<guid>https://blog.google/innovation-and-ai/models-and-research/gemini-models/introducing-gemini-3-7-flash/</guid>
<category>Gemini models</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Tulsee Doshi</name>
<title>Senior Director</title>
<department>Product Management, on behalf of the Gemini team</department>
<company/>
</author>
</item>
<item>
<title>Bring your spreadsheet data to life with Sheets canvas</title>
<link>https://blog.google/products-and-platforms/products/workspace/sheets-canvas-for-google-sheets-spreadsheets/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Sheets_canvas-blog-header-2784x.max-600x600.format-webp.webp" width="540"/>
<media:description>The video shows Sheets canvas in action.</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Sheets_canvas-blog-header-2784x.max-600x600.format-webp.webp">Sheets canvas turns data into interactive dashboards, custom study trackers, seating charts, and more, all with a simple prompt.</description>
<pubDate>Thu, 13 Aug 2026 16:45:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/products/workspace/sheets-canvas-for-google-sheets-spreadsheets/</guid>
<category>Google Workspace</category>
<category>Google One</category>
<category>AI</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Eric Birnbaum</name>
<title>Director, Product Management</title>
<department>Google Sheets</department>
<company/>
</author>
</item>
<item>
<title>Get updates while your Pixel 11 Pro is face down with HiLight.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-11-pro-hilight/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/KW_G_HiLite.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/KW_G_HiLite.max-600x600.format-webp.webp">HiLight, a new Pixel 11 Pro feature, gently glows to notify you of timely updates — keeping you informed when your phone is down.</description>
<pubDate>Thu, 13 Aug 2026 16:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-11-pro-hilight/</guid>
<category>Pixel</category>
</item>
<item>
<title>Stay present while taking pictures with Magic Capture on Pixel 11.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-11-magic-capture/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/MagicCapture.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/MagicCapture.max-600x600.format-webp.webp">Pixel 11’s new Magic Capture feature finds the best shots from dynamic scenes so you don’t miss precious moments trying to capture them.</description>
<pubDate>Thu, 13 Aug 2026 16:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-11-magic-capture/</guid>
<category>Pixel</category>
</item>
<item>
<title>Omni experts share what excites them most about the model.</title>
<link>https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-omni-experts-roundtable/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Omni_experts_social.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Omni_experts_social.max-600x600.format-webp.webp">We interviewed some of the experts behind Gemini Omni to hear what excites them most about the model.</description>
<pubDate>Thu, 13 Aug 2026 13:30:00 +0000</pubDate>
<guid>https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-omni-experts-roundtable/</guid>
<category>Gemini models</category>
<category>Google DeepMind</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Lindsey Lanquist</name>
<title>Contributor</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Sharing between Pixel devices is now just a tap away.</title>
<link>https://blog.google/products-and-platforms/platforms/android/tap-to-share-android/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Tap_to_Share_social.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Tap_to_Share_social.max-600x600.format-webp.webp">Sharing between Android devices is even easier, starting with Pixel. Use Quick Share and tap to send contact info and more.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/platforms/android/tap-to-share-android/</guid>
<category>Android</category>
<category>Pixel</category>
</item>
<item>
<title>Google’s most sophisticated foldable: Pixel 11 Pro Fold</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-11-pro-fold/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_11_Pro_Fold_Social.max-600x600.format-webp.webp" width="540"/>
<media:description>Video showing Pixel 11 Pro Fold, new features and upgrades</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_11_Pro_Fold_Social.max-600x600.format-webp.webp">Pixel 11 Pro Fold is our latest foldable with a thinner and lighter design, a new main camera, and enhanced intelligence tools.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-11-pro-fold/</guid>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Justin Savich</name>
<title>Group Product Manager</title>
<department/>
<company/>
</author>
</item>
<item>
<title>The Pixel 11 series: Your most personal Pixel yet</title>
<link>https://blog.google/products-and-platforms/devices/pixel/google-pixel-11-pro-xl/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Fudge_Sizzle-Grizzly_Thumbnail_.max-600x600.format-webp.webp" width="540"/>
<media:description>Close-up video of Pixel 11 Pro in Canyon</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Fudge_Sizzle-Grizzly_Thumbnail_.max-600x600.format-webp.webp">Discover the Google Pixel 11 lineup featuring updated camera hardware, the Tensor G6 chip, LED HiLight notifications and faster Gemini AI support.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/google-pixel-11-pro-xl/</guid>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Spencer Boone</name>
<title>Product Manager</title>
<department/>
<company/>
</author>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Akash Rathi</name>
<title>Product Manager</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Keep tabs on your valuables with Google Pixel Tag</title>
<link>https://blog.google/products-and-platforms/devices/pixel/google-pixel-tag/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/MBG2026_PixelTagHero.max-600x600.format-webp.webp" width="540"/>
<media:description>Pixel Tag attached to a keychain, resting inside a small trinket box.</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/MBG2026_PixelTagHero.max-600x600.format-webp.webp">Google Pixel Tag locates keys, wallets, and luggage via Bluetooth Channel Sounding and Find Hub. Check specs, price, and compatibility.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/google-pixel-tag/</guid>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Chris Yang Chen</name>
<title>Product Manager</title>
<department/>
<company/>
</author>
</item>
<item>
<title>Pixel Watch 5: Proactive assistance and advanced health tracking on your wrist</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-watch-5/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Active_Band_Olive_Flat_Lay_-_Ed.max-600x600.format-webp.webp" width="540"/>
<media:description>A close up video of the Pixel Watch 5</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Active_Band_Olive_Flat_Lay_-_Ed.max-600x600.format-webp.webp">Google Pixel Watch 5 offers proactive alerts, low-latency Gemini AI features, and health tracking to keep you connected.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-watch-5/</guid>
<category>Google Health</category>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Munjal Shah</name>
<title>Senior Product Manager</title>
<department>Pixel Watch</department>
<company/>
</author>
</item>
<item>
<title>Here’s your first look at the newest Pixel devices.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/new-pixel-devices-2026/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/newest_Pixel_devices_social_v2.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/newest_Pixel_devices_social_v2.max-600x600.format-webp.webp">Here’s a first look at our new Pixel devices, including new Pixel 11 phones, Pixel Watch 5, and our first-ever Pixel Tag.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/new-pixel-devices-2026/</guid>
<category>Pixel</category>
</item>
<item>
<title>Here are 3 ways your Pixel just got even more helpful.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-11-helpful-updates-features/</link>
<media:content height="540" medium="image" url="https://i.ytimg.com/vi_webp/nd5s2AUUHSc/maxresdefault.webp" width="540"/>
<media:description>Shakil Barkat, vice president of devices and services, shares his favorite Pixel 11 features</media:description>
<description><img src="https://i.ytimg.com/vi_webp/nd5s2AUUHSc/maxresdefault.webp">Our Pixel 11 phones are packed with helpful updates, like Magic Capture, Rambler, and HiLight. Here, a Googler shares his favorites.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-11-helpful-updates-features/</guid>
<category>Pixel</category>
</item>
<item>
<title>New accessories for Pixel 11 phones are here.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/new-pixelsnap-accessories-cases-ring-stand/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Accessories.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Accessories.max-600x600.format-webp.webp">Google is rolling out new Pixel accessories as part of Made by Google</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/new-pixelsnap-accessories-cases-ring-stand/</guid>
<category>Pixel</category>
</item>
<item>
<title>Here’s what’s new with Pixel Buds Pro 2 and Pixel Buds 2a.</title>
<link>https://blog.google/products-and-platforms/devices/pixel/google-pixel-buds-mbg-2026/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_Buds_Pro_2_and_Pixel_Buds.max-600x600.format-webp.webp" width="540"/>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/Pixel_Buds_Pro_2_and_Pixel_Buds.max-600x600.format-webp.webp">Google is launching a new color and upgraded features for Pixel Buds Pro 2 and Pixel Buds 2a as part of Made by Google.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/google-pixel-buds-mbg-2026/</guid>
<category>Pixel</category>
</item>
<item>
<title>Track subtle changes in your body with Health Guardian features on Pixel and Fitbit</title>
<link>https://blog.google/products-and-platforms/products/google-health/pixel-watch-health-guardian/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/HealthGuardian_social.max-600x600.format-webp.webp" width="540"/>
<media:description>The video shows Health Guardian features on a phone.</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/HealthGuardian_social.max-600x600.format-webp.webp">A new level of proactive health and wellness is coming to your wrist.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/products/google-health/pixel-watch-health-guardian/</guid>
<category>Google Health</category>
<category>Fitbit</category>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Florence Thng</name>
<title>Health Intelligence Product Management Director</title>
<department/>
<company/>
</author>
</item>
<item>
<title>How we built the new GPS on Pixel Watch 5</title>
<link>https://blog.google/products-and-platforms/devices/pixel/pixel-watch-5-gps/</link>
<media:content height="540" medium="image" url="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/GPS_on_the_Pixel_Watch_5_Hero.max-600x600.format-webp.webp" width="540"/>
<media:description>Man jogging wearing Pixel Watch 5</media:description>
<description><img src="https://storage.googleapis.com/gweb-uniblog-publish-prod/images/GPS_on_the_Pixel_Watch_5_Hero.max-600x600.format-webp.webp">The Pixel Watch 5 offers our most accurate GPS route tracking yet, helping you retrace where you went on runs, hikes, and bike rides.</description>
<pubDate>Wed, 12 Aug 2026 14:00:00 +0000</pubDate>
<guid>https://blog.google/products-and-platforms/devices/pixel/pixel-watch-5-gps/</guid>
<category>Pixel</category>
<author xmlns:author="http://www.w3.org/2005/Atom">
<name>Lindsey Lanquist</name>
<title>Contributor</title>
<department/>
<company/>
</author>
</item>
</channel>
</rss>
    """.trimIndent()
}
