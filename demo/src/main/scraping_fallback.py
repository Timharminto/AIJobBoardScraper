import sys
import json
from scrapling import Fetcher

def run_scraper(url):
    try:
        # Initialize Scrapling Fetcher
        fetcher = Fetcher(url)
        
        # Use Scrapling's engine to bypass bot protection and render the page
        page = fetcher.auto_match_engine()
        
        # Option A: You can parse the jobs right here in Python and print a JSON array
        # Option B: Just print the fully rendered HTML and let Java/Jsoup parse it
        rendered_html = page.html
        
        # Print the HTML so Java can read it from the standard output
        print(rendered_html)
        
    except Exception as e:
        print(f"ERROR: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        target_url = sys.argv[1]
        run_scraper(target_url)