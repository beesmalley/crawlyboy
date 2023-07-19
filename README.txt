CAPTAINS' LOGS
Feel free to add update notes here. Add the date and your name so we know who's writing. 


7/13/23 BEETHOVEN
    howdy ya'll

    So far we've got a basic web crawler with a GUI! 
    It's a Maven project and I think it's using a separate thread to do the actual crawling while 
    the main thread sets up the GUI stuff. I could be completely making that up lmao 
    I'm planning on working more on the multi threaded aspect as time goes on. Also right now it 
    shows data from each web page that it visits by scanning the meta html tags and recording the Title, 
    Description, Keywords, and the URL. 

    Things that still need work:
        - The multi threading
        - A lot of websites don't use the meta tags for keywords and some websites don't have those 
        meta tags at all, so some of the entries show up blank. Not sure what a good solution 
        is for this yet but I'm sure there is one
        - She is a pretty ugly GUI and I just think we could make her look better
        - Right now she just prints text into the text area of the GUI, we should probably add a 
        button to export all the data from that crawl to a JSON or some other object document.
        - She is only executable from the terminal currently. So I'm also trying to add like an 
        executable jar as a application icon so running it is more straight forward

    ---------------------
    To run this project from the terminal, make sure you have Apache Maven and JSoup installed. (The JSoup libraries
    help us a lot with the actual web interaction.) In the terminal, navigate to the crawler folder and then run the
    commands:

    mvn clean
    mvn compile exec:java -Dexec.mainClass="osproject.App"

    That should prompt a GUI to appear with a text field for you to put the starting URL. It must start with the
    full URL (example: "http://google.com" instead of just "google.com"). Then use the start and stop buttons to 
    resume and pause this process.

    I suggest creating a new branch or using the "prototype" branch to make changes and test them and then later 
    merging to main. 

7/19/23 BEETHOVEN  

    hoping to make an export button today so instead of just seeing the web data printed onto the gui, 
    we can export that information into a JSON. Also! Since we are using javax.swing, i can confirm the
    application is indeed multithreaded. woohoo! 

    UPDATE: Export button has been added. User is able to choose where to export the file to and it will
    create a JSON with the object data for the WebsiteInfo object.

    Things left to do:
        - A lot of websites don't use the meta tags for keywords and some websites don't have those 
        meta tags at all, so some of the entries show up blank. Not sure what a good solution 
        is for this yet but I'm sure there is one
        - She is a pretty ugly GUI and I just think we could make her look better
        - She is only executable from the terminal currently. So I'm also trying to add like an 
        executable jar as a application icon so running it is more straight forward

