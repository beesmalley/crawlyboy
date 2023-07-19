To run this project from the terminal, make sure you have Apache Maven and JSoup installed. (The JSoup libraries
help us a lot with the actual web interaction.) In the terminal, navigate to the crawler folder and then run the
commands:

mvn clean
mvn compile exec:java -Dexec.mainClass="osproject.App"

That should prompt a GUI to appear with a text field for you to put the starting URL. It must start with the
full URL (example: "http://google.com" instead of just "google.com"). Then use the start and stop buttons to 
resume and pause this process. The export button will export all the website data to a JSON in the location
of your choice.