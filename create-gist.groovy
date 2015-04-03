@Grab('net.sourceforge.nekohtml:nekohtml:1.9.15')
import groovy.transform.ToString

def sitemapUrl = 'http://www.hascode.com/sitemap.xml'
def articlePattern = /.*\d{4}\/\d{2}\/.+/

@ToString
class Article {
    String url
    String title
    List<String> tags = []
    
    def printTags = {
        tags.collect{"'$it'"}.join(', ')
    }
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

def tag2Key = {
    "tag_"+it.replaceAll(' ','_').replaceAll('-','_').replaceAll(/\./,'_')
}

/**
 * writes the cypher scripts.
 **/
def writeCypher = {
    println("// creating cypher queries for ${articles.size()} articles")
    println("// creating tags")
    articles.collect{it.tags}.flatten().unique().each { tag ->
        println("CREATE (${tag2Key(tag)}:TAG{title:'$tag'})")
    }
    println("// creating article nodes and relations")
    articles.eachWithIndex { article, idx ->
        println "CREATE (p$idx:ARTICLE{title:'${article.title}', url:'${article.url}' ,tags:[${article.printTags()}]})"
        article.tags.each { tag ->
            println("CREATE (p$idx)-[:HAS_TAG]->(${tag2Key(tag)})")
        }
    }
}

/**
 * parses the article's meta-data
 **/
def parseArticle = { url ->
    def page = new XmlSlurper(new org.cyberneko.html.parsers.SAXParser()).parse(url)
    def title = getTitle(page)
    def tags = getTags(page)
    
    articles.add(new Article(url:url, title:title, tags:tags))
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
    writeCypher()
}

println("// import started: ${new Date()}")
run()
println("// import completed: ${new Date()}")