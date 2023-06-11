/*  Klasa agenta kupującego lektury w imieniu właściciela
 *
 *  Argumenty projektu (NETBEANS: project properties/run/arguments):
 *  -agents seller1:BookSellerAgent();seller2:BookSellerAgent();buyer1:BookBuyerAgent(Zamek) -gui
 */

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

// Przykładowa klasa zachowania:
class MyOwnBehaviour extends Behaviour
{
    protected MyOwnBehaviour()
    {
    }

    public void action()
    {
    }
    public boolean done() {
        return false;
    }
}

public class BookBuyerAgent extends Agent {

    private String targetBookTitle;    // tytuł kupowanej książki przekazywany poprzez argument wejściowy
    // lista znanych agentów sprzedających książki (w przypadku użycia żółtej księgi - usługi katalogowej, sprzedawcy
    // mogą być dołączani do listy dynamicznie!
    private AID[] sellerAgents = {
            new AID("seller1", AID.ISLOCALNAME),
            new AID("seller2", AID.ISLOCALNAME)};

    int offerNumb = 0;

    // Inicjalizacja klasy agenta:
    protected void setup()
    {

        //doWait(2023);   // Oczekiwanie na uruchomienie agentów sprzedających

        System.out.println("Witam! Agent-kupiec "+getAID().getName()+" (wersja E 2020/21) jest gotowy!");

        Object[] args = getArguments();  // lista argumentów wejściowych (tytuł książki)

        if (args != null && args.length > 0)   // jeśli podano tytuł książki
        {
            targetBookTitle = (String) args[0];
            System.out.println("Zamierzam kupić książkę zatytułowaną "+targetBookTitle);

            addBehaviour(new RequestPerformer());  // dodanie głównej klasy zachowań - kod znajduje się poniżej

        }
        else
        {
            // Jeśli nie przekazano poprzez argument tytułu książki, agent kończy działanie:
            System.out.println("Proszę podać tytuł lektury w argumentach wejściowych agenta kupującego!");
            doDelete();
        }
    }
    // Metoda realizująca zakończenie pracy agenta:
    protected void takeDown()
    {
        System.out.println("Agent-kupiec "+getAID().getName()+" skończył.");
    }

    /**
     Inner class RequestPerformer.
     This is the behaviour used by Book-buyer agents to request seller
     agents the target book.
     */
    private class RequestPerformer extends Behaviour
    {

        private AID bestSeller;     // agent sprzedający z najkorzystniejszą ofertą
        private int bestPrice;      // najlepsza cena
        private int repliesCnt = 0; // liczba odpowiedzi od agentów
        private MessageTemplate mt; // szablon odpowiedzi
        private int step = 0;       // krok
        private int wantedPrice;

        public void action()
        {
            switch (step) {
                case 0:      // wysłanie oferty kupna
                    System.out.print(" Oferta kupna (CFP) jest wysyłana do: ");
                    for (int i = 0; i < sellerAgents.length; ++i)
                    {
                        System.out.print(sellerAgents[i]+ " ");
                    }
                    System.out.println();

                    // Tworzenie wiadomości CFP do wszystkich sprzedawców:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i)
                    {
                        cfp.addReceiver(sellerAgents[i]);                // dodanie adresata
                    }
                    cfp.setContent(targetBookTitle);                   // wpisanie zawartości - tytułu książki
                    cfp.setConversationId("handel_lekturami");         // wpisanie specjalnego identyfikatora korespondencji
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // dodatkowa unikatowa wartość, żeby w razie odpowiedzi zidentyfikować adresatów
                    myAgent.send(cfp);                           // wysłanie wiadomości

                    // Utworzenie szablonu do odbioru ofert sprzedaży tylko od wskazanych sprzedawców:
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_lekturami"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;     // przejście do kolejnego kroku
                    break;
                case 1:      // odbiór ofert sprzedaży/odmowy od agentów-sprzedawców
                    ACLMessage reply = myAgent.receive(mt);      // odbiór odpowiedzi
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.PROPOSE)   // jeśli wiadomość jest typu PROPOSE
                        {
                            int price = Integer.parseInt(reply.getContent());  // cena książki
                            if (bestSeller == null || price < bestPrice)       // jeśli jest to najlepsza oferta
                            {
                                bestPrice = price;
                                bestSeller = reply.getSender();
                                wantedPrice = (int)(bestPrice*0.8);
                            }
                        }
                        repliesCnt++;                                        // liczba ofert
                        if (repliesCnt >= sellerAgents.length)               // jeśli liczba ofert co najmniej liczbie sprzedawców
                        {
                            step = 2;
                        }
                    }
                    else
                    {
                        block();
                    }
                    break;
                case 2:      // wysłanie propozycji zamówienia do sprzedawcy, który złożył najlepszą ofertę
                    if(offerNumb >=6) {
                        System.out.println("Book Seller refused sell offer " + targetBookTitle + ".");
                        myAgent.doDelete();
                        step = 4;
                        break;
                    }
                    System.out.println(offerNumb);
                    System.out.println("Buyer Agent proposed: " + wantedPrice
                            + " for " + targetBookTitle + ".");


                    ACLMessage order = new ACLMessage(ACLMessage.PROPOSE);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle +";"+ wantedPrice);
                    order.setConversationId("handel_lekturami");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_lekturami"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 5;
                    break;
                case 3:      // odbiór odpowiedzi na zamównienie
                    reply = myAgent.receive(mt);
                    if (reply != null)
                    {
                        if (reply.getPerformative() == ACLMessage.INFORM)
                        {
                            System.out.println("Tytuł "+targetBookTitle+" zamówiony!");
                            System.out.println("Po cenie: "+wantedPrice);
                            myAgent.doDelete();
                        }
                        step = 4;
                    }
                    else
                    {
                        block();
                    }
                    break;
                case 5:
                    // new price
                    reply = myAgent.receive(mt);

                    if(reply == null) {
                        break;
                    }
                    if(reply.getPerformative() == ACLMessage.REFUSE) {
                        System.out.println(reply.getContent());
                        step = 4;
                        break;
                    }
                    bestPrice = Integer.parseInt(reply.getContent());

                    if((bestPrice - wantedPrice) <= 2) {
                        step = 6;
                        break;
                    } else {
                        offerNumb++;
                        System.out.println(offerNumb);
                        wantedPrice = (wantedPrice * 3 / 4) + (bestPrice / 4);
                        step = 2;
                        break;
                    }
                case 6:
                    // ACCEPT_PROPOSAL
                    ACLMessage order1 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order1.addReceiver(bestSeller);
                    order1.setContent(targetBookTitle);
                    order1.setConversationId("handel_lekturami");
                    order1.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order1);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("handel_lekturami"),
                            MessageTemplate.MatchInReplyTo(order1.getReplyWith()));

                    System.out.println("Final offer of " + targetBookTitle + " for: " + wantedPrice);

                    step = 3;
                    break;

            }  // switch
        } // action

        public boolean done() {
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    } // Koniec wewnętrznej klasy RequestPerformer
}
