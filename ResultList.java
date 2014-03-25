package cz.ctu.fit.ddw;

import java.util.List;

public class ResultList {

    private InternList responseData;

    /**
     *
     * @return
     */
    public InternList getResponseData() {
        return responseData;
    }

    /**
     *
     * @param responseData
     */
    public void setResponseData(InternList responseData) {
        this.responseData = responseData;
    }

    static class InternList {

        private List<Result> results;

        public List<Result> getResults() {
            return results;
        }

        public void setResults(List<Result> results) {
            this.results = results;
        }
    }

    static class Result {

        private String url;
        private String title;

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

}
