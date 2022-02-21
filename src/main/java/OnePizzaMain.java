import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class OnePizzaMain {

    private static Map<String, Integer> likeIngredients = new HashMap<>();
    private static Map<String, Integer> disLikeIngredients = new HashMap<>();
    static List<Client> satisfied = new ArrayList<>();

    private static List<String[]> fileToStringList(Path path) {
        List<String> list;
        if (Files.exists(path)) {
            try {
                list = Files.lines(path).toList();
            } catch (IOException e) {
                return null;
            }
        } else {
            System.out.println("Create and fill file first.");
            return null;
        }

        return list.stream().map(x -> x.split(" ")).toList();
    }

    private static List<Client> fillClientsList(List<String[]> trimString) {

        List<Client> clients = new ArrayList<>();

        for (int i = 1; i < trimString.size(); i += 2) {
            Set<String> allUniqLikeProducts = new HashSet<>();
            Set<String> allUniqDislikeProducts = new HashSet<>();
            Client client = new Client();

            String[] product = trimString.get(i);
            client.setLikesSize(product.length - 1);
            for (int j = 1; j < product.length; j++) {
                allUniqLikeProducts.add(product[j]);
                addToLikeStatistic(product[j]);
            }
            client.setLikes(allUniqLikeProducts);

            String[] product2 = trimString.get(i + 1);
            client.setDislikesSize(product2.length - 1);
            for (int k = 1; k < product2.length; k++) {
                allUniqDislikeProducts.add(product2[k]);
                addToDislikeStatistic(product2[k]);
            }
            client.setDislikes(allUniqDislikeProducts);
            if (client.getDislikesSize() == 0) {
                client.setEfficient(client.getLikesSize());
            } else {
                client.setEfficient(Math.round(client.getLikesSize() / client.getDislikesSize()));
            }
            client.setOpinionSize(client.getLikesSize() + client.getDislikesSize());
            clients.add(client);
        }

        likeIngredients =
                likeIngredients.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                        LinkedHashMap::new));
        disLikeIngredients =
                disLikeIngredients.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(
                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                        LinkedHashMap::new));

        return clients;
    }
    private static void addToDislikeStatistic(String s) {
        if (disLikeIngredients.containsKey(s)) {
            disLikeIngredients.put(s, disLikeIngredients.get(s) + 1);
        } else {
            disLikeIngredients.put(s, 1);
        }
    }

    private static void addToLikeStatistic(String s) {
        if (likeIngredients.containsKey(s)) {
            likeIngredients.put(s, likeIngredients.get(s) + 1);
        } else {
            likeIngredients.put(s, 1);
        }
    }

    private static List<Client> reEvaluateClients(List<Client> clients) {

        System.out.println(disLikeIngredients.size());

        for (Client client : clients) {
            int disLikeScore = 0;
            if (disLikeIngredients.size() == 0) {
                client.setDisLikeScore(disLikeScore);
                continue;
            }
            for (String like : client.getLikes()) {
                for (Map.Entry<String, Integer> entry : disLikeIngredients.entrySet()) {
                    if (entry.getKey().equals(like)) {
                        disLikeScore += entry.getValue();
                    }
                }
            }
            client.setDisLikeScore(disLikeScore);
        }

        clients = clients.stream().sorted(Client.Comparators.SCORE).toList();
        //clients.stream().forEach(System.out::println);

        return new ArrayList<>(clients);
    }

    private static List<Client> checkSatisfied(List<Client> clients, Set<String> ingredients) {

        for (Client client : clients) {
            if (ingredients.containsAll(client.getLikes())) {
                satisfied.add(client);
                clients.remove(client);
            }
        }

        return clients;
    }

    private static List<Client> cleanClients(List<Client> clients, Set<String> ingredients) {
        boolean delete = false;

        for (Client client : clients) {
            for (String dislike : client.getDislikes()) {
                for (String ingredient : ingredients) {
                    if (ingredient.equals(dislike)) {
                        delete = true;
                        break;
                    }
                }
                if (delete) break;
            }
            if (delete) {
                clients.remove(client);
                delete = false;
            }
        }

        return clients;
    }

    private static Map<String, Integer> cleanDislikes(Set<String> ingredients, Map<String, Integer> dislikes) {
        for (String ingredient : ingredients) {
            for (Map.Entry<String, Integer> entry : dislikes.entrySet()) {
                if (entry.getKey().equals(ingredient)) dislikes.remove(entry.getKey());
            }
        }

        return new HashMap<>(dislikes);
    }

    private static Set<String> processIngredients(List<Client> clients) {
        Set<String> result = new HashSet<>();
        Map<String, Integer> dislikes = new HashMap<>(disLikeIngredients);

        do {
            clients = reEvaluateClients(clients);
            satisfied.add(clients.get(0));
            result.addAll(clients.get(0).getLikes());
            System.out.println(clients.get(0).getDisLikeScore());
            if (clients.get(0).getDisLikeScore() > 0) clients = cleanClients(clients, result);
            clients.remove(0);
            clients = checkSatisfied(clients, result);
            System.out.println("Clients satisfied: " + satisfied.size());
            dislikes = cleanDislikes(result, dislikes);
        } while (!clients.isEmpty());

        return result;
    }

    private static void output(Set<String> ingredients, String out) {
        Path path = Paths.get(out);
        String result = "";

        int count = ingredients.size();
        result = count + " ";

        for (String s : ingredients) {
            result += s + " ";
        }

        result = result.substring(0, result.length() - 1);

        if (Files.exists(path)) {
            try {
                Files.delete(path);
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.write(path, result.getBytes(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Path path = Paths.get("c_coarse.in.txt");
        output(processIngredients(fillClientsList(fileToStringList(path))), "c_output.txt");
    }

    static class Client implements Comparable<Client> {

        private Set<String> likes;
        private double likesSize;
        private Set<String> dislikes;
        private double dislikesSize;
        private double efficient;
        private double opinionSize;
        private Integer disLikeScore;

        public Integer getDisLikeScore() {
            return disLikeScore;
        }

        public void setDisLikeScore(Integer disLikeScore) {
            this.disLikeScore = disLikeScore;
        }

        public double getOpinionSize() {
            return opinionSize;
        }

        public void setOpinionSize(double opinionSize) {
            this.opinionSize = opinionSize;
        }

        public double getLikesSize() {
            return likesSize;
        }

        public void setLikesSize(double likesSize) {
            this.likesSize = likesSize;
        }

        public double getDislikesSize() {
            return dislikesSize;
        }

        public void setDislikesSize(double dislikesSize) {
            this.dislikesSize = dislikesSize;
        }

        public double getEfficient() {
            return efficient;
        }

        public void setEfficient(double efficient) {
            this.efficient = efficient;
        }

        public Client() {
        }

        public Set<String> getLikes() {
            return likes;
        }

        public void setLikes(Set<String> likes) {
            this.likes = likes;
        }

        public Set<String> getDislikes() {
            return dislikes;
        }

        public void setDislikes(Set<String> dislikes) {
            this.dislikes = dislikes;
        }

        @Override
        public int compareTo(Client o) {

            return Comparators.OP_AND_EFF.compare(this, o);
        }

        public static class Comparators {
            public static final Comparator<Client> OP_SIZE = Comparator.comparingDouble(Client::getOpinionSize);
            public static final Comparator<Client> EFFICIENT = Comparator.comparingDouble(Client::getEfficient);
            public static final Comparator<Client> SCORE = Comparator.comparingInt(Client::getDisLikeScore);
            public static final Comparator<Client> OP_AND_EFF = (Client o1, Client o2) -> EFFICIENT.thenComparing(OP_SIZE).compare(o1, o2);
        }

        @Override
        public String toString() {
            return "Client{" +
                    "L:" + likesSize +
                    " D:" + dislikesSize +
                    " E:" + efficient +
                    " O:" + opinionSize +
                    " S:" + disLikeScore +
                    '}' + "\n";
        }
    }
}

