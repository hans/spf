object :- N : scene:<e,t>
thing :- N : scene:<e,t>

metallic :- N/N : (lambda $0:<e,t> (filter_material:<<e,t>,<p,<e,t>>> $0 metal:p))
shiny :- N/N : (lambda $0:<e,t> (filter_material:<<e,t>,<p,<e,t>>> $0 shiny:p))
big :- N/N : (lambda $0:<e,t> (filter_size:<<e,t>,<p,<e,t>>> $0 large:p))
green :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<p,<e,t>>> $0 green:p))
brown :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<p,<e,t>>> $0 brown:p))
purple :- N/N : (lambda $0:<e,t> (filter_color:<<e,t>,<p,<e,t>>> $0 purple:p))

the :- S/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))
the :- N/N : (lambda $0:<e,t> (unique:<<e,t>,e> $0))

other things that are the same shape as :- N/N : (lambda $0:e (same_shape:<e,<e,t>> $0))
//of the same shape as :- A/N : (lambda $0:e (same_shape:<e,<e,t>> $0))
the same size as :- N/N : (lambda $0:e (same_size:<e,<e,t>> $0))

are there any :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
is there a :- S/N : (lambda $0:<e,t> (exists:<<e,t>,t> $0))
what is the material of :- S/N : (lambda $0:e (query_material:<e,p> $0))
what number of other objects are :- S/N : (lambda $0:<e,t> (count:<<e,t>,i> $0))