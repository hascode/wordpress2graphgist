@Grab('net.sourceforge.nekohtml:nekohtml:1.9.15')
import groovy.transform.ToString

def sitemapUrl = 'http://www.hascode.com/sitemap.xml'
def articlePattern = /.*\d{4}\/\d{2}\/.+/

@ToString
class Article {
    String url
    String title
    List<String> tags = []
}

def articles = []

/**
 * fetches the article's title
 **/
def getTitle = { page ->
    page?.'**'?.find{it.name().equals("H2")}.text()
}

/**
 * fetches the article's tags
 **/
def getTags = { page ->
    def parent = page?.'**'.find{
        it.name().equals("P") && it.text().startsWith('Tags:')
    }
    if(parent == null){
        return []
    }
    
    parent.'**'.findAll { 
        it.name().equals("A") 
    }.collect {it.text().toLowerCase()}.sort()
}

/**
 * writes the cypher scripts.
 **/
def writeCypher = {
    println("// creating cypher queries for ${articles.size()} articles")
}

/**
 * parses the article's meta-data
 **/
def parseArticle = { url ->
    def page = new XmlSlurper(new org.cyberneko.html.parsers.SAXParser()).parse(url)
    def title = getTitle(page)
    def tags = getTags(page)
    
    articles.add(new Article(url:url, title:title, tags:tags))
    writeCypher()
    throw new IllegalArgumentException()
}
def memoParseArticle = parseArticle.memoize()

def run = {
    def sitemapXml = new XmlParser().parse(sitemapUrl)
    sitemapXml.children().findAll{
        it?.loc?.text() != null
    }.collect{
        it.loc.text()
    }.findAll{it =~ articlePattern}.each {
        memoParseArticle(it)
    }
}

println("// import started: ${new Date()}")
run()
println("// import completed: ${new Date()}")